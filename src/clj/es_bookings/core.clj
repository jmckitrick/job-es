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
  (let [;;hostname (or (System/getenv "DB_HOST") "mysqlread.prod.infra.tstllc.net")
        ;;username (or (System/getenv "DB_USER") "support_ro")
        ;;password (or (System/getenv "DB_PASS") "1mdsupp0rtp3rs0n!")
        hostname (or (System/getenv "DB_HOST") "nodes.lower.kube.tstllc.net")
        username (or (System/getenv "DB_USER") "root")
        password (or (System/getenv "DB_PASS") "kube-aws")
        port 32110]
    {:dbtype "mysql"
     :dbname "book"
     :user username
     :password password
     :host hostname
     :port 32110
     :ssl false}))

(defqueries "travel-booking.sql" {:connection (get-db-spec)})

;;(def c (s/client {:hosts ["http://localhost:8000"]}))
(def c (s/client {:hosts ["http://nodes.lower.kube.tstllc.net:32200"]}))

(defn get-booking-type [row]
  (cond
    (and (empty? (:subsite row))
         (not-empty (:agent row))) "agent"
    (and (empty? (:agent row))
         (not-empty (:subsite row))) "personal subsite"
    :else "web"))

(defn add-to-index-bulk-async [rows]
  (let [{:keys [input-ch output-ch]} (s/bulk-chan c {:flush-threshold 100
                                                     :flush-interval 5000
                                                     :max-concurrent-requests 1000})]
    (doseq [row-set (partition-all 1000 rows)]
      ;;(println "Importing " (count row-set))
      (doseq [row row-set]
        (let [ready-row (assoc row :booking_type (get-booking-type row))]
          (async/>!! input-ch [{:index  ;desired ES action
                                {:_index :booking ;name of index
                                 :_type :_doc ;add to documents on index
                                 :_id (:id row)}}
                               ready-row]))))
    (future (loop [] (async/<!! output-ch)))))

(defn -main [year start-month end-month]
  (let [start-date (str year "-" start-month "-01 00:00:00")
        end-date (str year "-" end-month "-01 00:00:00")
        bookings (get-travel-bookings {:start_date start-date
                                       :end_date end-date})]
    (println "Bookings: " (count bookings))
    #_(println "Bookings: " bookings)
    (add-to-index-bulk-async bookings)))
