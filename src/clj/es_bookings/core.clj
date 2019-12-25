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
                                                     :max-concurrent-requests 1000})]
    (doseq [row-set (partition-all 1000 rows)]
      (println "Importing" (count row-set) "to" index)
      (doseq [row row-set]
        (let [ready-row (assoc row
                               :booking_type (get-booking-type row))]
          (async/>!! input-ch [{:index  ;desired ES action
                                {:_index index ;name of index
                                 :_type :_doc ;add to documents on index
                                 :_id (:id row)}}
                               ready-row]))))
    (future (loop [n 0]
              (println "N:" n)
              (if (= n (count rows))
                n
                (let [result (async/<!! output-ch)]
                  (recur (+ n (count (first result))))))))))

(defn -main [env year]
  (let [start-date (str year "-01-01 00:00:00")
        end-date (str year "-12-01 00:00:00")
        bookings (get-travel-bookings {:start_date start-date
                                       :end_date end-date})]
    (println "Env" env "Start" start-date "End" end-date)
    (println "Bookings:" (count bookings))
    #_(println "Bookings: " bookings)
    (let [result (add-to-index-bulk-async bookings env)]
      (println "Imported" @result "records.")
      (when (not (empty? *command-line-args*))
        (System/exit 0)))))
