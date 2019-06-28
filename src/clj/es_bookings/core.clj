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
  (let [hostname (or (System/getenv "DB_HOST") "mysqlread.prod.infra.tstllc.net")
        username (or (System/getenv "DB_USER") "support_ro")
        password (or (System/getenv "DB_PASS") "1mdsupp0rtp3rs0n!")]
    {:dbtype "mysql"
     :dbname "book"
     :user username
     :password password
     :host hostname
     :ssl false
     }))

(defqueries "travel-booking.sql" {:connection (get-db-spec)})

(def c (s/client {:hosts ["http://localhost:8000"]}))

(defn get-booking-type [row]
  (cond
    (and (not-empty (:agent row))
         (empty? (:subsite row))) "agent"
    (and (not-empty (:subsite row))
         (empty? (:agent row))) "personal subsite"
    :else "web"))

(defn add-to-index-bulk-async [rows]
  (let [{:keys [input-ch output-ch]} (s/bulk-chan c {:flush-threshold 100
                                                     :flush-interval 5000
                                                     :max-concurrent-requests 1000})]
    (doseq [row-set (partition-all 1000 rows)]
      (doseq [idx (range 0 (count row-set))]
        (let [row (nth row-set idx)
              ready-row (assoc row :type (get-booking-type row))]
          (async/>!! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} ready-row]))))
    (future (loop [] (async/<!! output-ch)))))

(defn -main []
  (let [bookings (get-travel-bookings)]
    (println "Bookings: " (count bookings))
    #_(println "Bookings: " bookings)
    (add-to-index-bulk-async bookings)))
