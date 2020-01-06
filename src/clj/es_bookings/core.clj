(ns es-bookings.core
  (:gen-class)
  (:require
   [clojure.string :as string]
   [clojure.core.async :as async]
   [qbits.spandex :as s]
   [yesql.core :refer [defqueries]]))

(defn get-db-spec
  "Get a database connection spec.
  Prefer env variables then fall back to defaults."
  []
  (let [hostname (or (System/getenv "DB_HOST") "127.0.0.1")
        username (or (System/getenv "DB_USER") "root")
        password (or (System/getenv "DB_PASS") "")
        port (or (System/getenv "DB_PORT") 3306)]
    (println "Using" hostname "as db host.")
    {:dbtype "mysql"
     :dbname "book"
     :user username
     :password password
     :host hostname
     :port port
     :ssl false}))

(defqueries "travel-booking.sql" {:connection (get-db-spec)})

(defn get-es "Direct to lower"
  []
  (s/client {:hosts ["http://localhost:8000"]})
  #_(s/client {:hosts ["http://nodes.lower.kube.tstllc.net:32200"]}))

(defn get-booking-type [row]
  (cond
    (and (empty? (:subsite row))
         (not-empty (:agent row))) "agent"
    (and (empty? (:agent row))
         (not-empty (:subsite row))) "agent site"
    :else "web"))

(defn add-to-index-bulk-async [rows env]
  (let [c (get-es)
        index (str "booking-" env)
        {:keys [input-ch output-ch]} (s/bulk-chan c {:flush-threshold 100
                                                     :flush-interval 5000
                                                     :max-concurrent-requests 1024})]
    (doseq [row-set (partition-all 1000 rows)]
      ;;(println "Importing" (count row-set) "to" index)
      (doseq [row row-set]
        (let [ready-row (assoc row
                               :booking_type (get-booking-type row))]
          #_(println "Inserting....")
          (async/>!! input-ch [{:index  ;desired ES action
                                {:_index index ;name of index
                                 :_type :_doc ;add to documents on index
                                 :_id (:id row)}}
                               ready-row]))
        #_(println "Imported a row"))
      #_(println "Imported a row set"))
    #_(println)
    (println "Imported all data in batch. Waiting on future.")
    (future (loop [n 0]
              (println "N:" n)
              ;;(print ".")
              (if (= n (count rows))
                n
                (let [result (async/<!! output-ch)]
                  (recur (+ n (count (first result))))))))))

(defn import-bookings-impl [env start-date end-date]
  (let [bookings (get-travel-bookings {:start_date start-date
                                       :end_date end-date})]
    (println "Env" env "Start" start-date "End" end-date)
    (println "Bookings:" (count bookings))
    (when (> (count bookings) 50000)
      (println "----> OVER 50000!"))
    #_(println "Bookings: " bookings)
    (let [result (add-to-index-bulk-async bookings env)]
      (println "Imported" @result "records.")
      (when (not (empty? *command-line-args*))
        (System/exit 0)))))

(defn import-bookings
  ([env year]
   (let [start-date (str year "-01-01 00:00:00")
         end-date (str (inc (read-string year)) "-01-01 00:00:00")]
     (import-bookings-impl env start-date end-date)))
  ([env year month]
   (let [start-date (str year "-" month "-01 00:00:00")
         end-date (str year "-" (inc (min (read-string month) 12)) "-01 00:00:00")]
     (import-bookings-impl env start-date end-date))))

(defn -main [& rest]
  (when (not (empty? *command-line-args*))
    (let [[env year month] *command-line-args*]
      (if month
        (import-bookings env year month)
        (import-bookings env year)))
    (System/exit 0)))
