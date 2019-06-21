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

(defn my-search []
  (s/request c {:url [:booking :_search ]
                :method :get
                :body {:query {:match_all {}}}}))

(defn add-to-index [{:keys [:id :fname :phone :branch :product :agent :email
                            :adapter :lname :subsite_agent_name :ref_conf_number :itinerary] :as row}]
  (s/request c {:url [:booking :_doc id]
                :method :put
                :body row}))

(defn add-to-index-bulk-async [rows]
  (let [{:keys [input-ch output-ch]} (s/bulk-chan c {:flush-threshold 100
                                                     :flush-interval 5000
                                                     :max-concurrent-requests 1000})]
    ;; WORKING with 10000 rows 100 5000 100
    (doseq [row-set (partition-all 1000 rows)]
      (println (count row-set))
      (doseq [idx (range 0 (count row-set))]
        ;;(println idx)
        (let [row (nth row-set idx)]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))))
    (future (loop [] (async/<!! output-ch)))))

(defn -main []
  (let [bookings (get-travel-bookings)]
    (println "Bookings: " (count bookings))
    #_(doseq [booking bookings]
        (add-to-index booking))
    (add-to-index-bulk-async bookings)))
