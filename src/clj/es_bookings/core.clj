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
                                                     :max-concurrent-requests 100})]
    ;; THIS WORKS FOR 2
    ;;(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id 1}} (first rows)])
    ;;(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id 2}} (second rows)])
    ;; THIS WORKS
    #_(doseq [idx (range 1 10)]
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id idx}} (nth rows idx)]))
    ;; THIS WORKS FOR 200
    #_(doseq [row-set (partition 100 rows)]
      (doseq [idx (range 0 100)]
        (let [row (nth row-set idx)]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))))
    ;;(println (count rows))
    ;; WORKING WITH 2000 rows and 100 5000 20 above
    #_(doseq [row-set (partition 100 rows)]
      (println (count row-set))
      (doseq [idx (range 0 100)]
        (println idx)
        (let [row (nth row-set idx)]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))))
    ;; WORKING with 10000 rows 100 5000 100
    (doseq [row-set (partition 1000 rows)]
      ;;(println (count row-set))
      (doseq [idx (range 0 1000)]
        ;;(println idx)
        (let [row (nth row-set idx)]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))))

    ;; FAILS AT 1024 limit
    #_(doseq [row rows]
        (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))
    ;; FAILS AT 4000 rows, ok at 2000 NO
    #_(doseq [[row1 row2] (partition 2 rows)]
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row1)}} row1])
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row2)}} row2]))

    #_(doseq [[row1 row2 row3 row4] (partition 4 rows)]
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row1)}} row1])
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row2)}} row2])
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row3)}} row3])
      (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row4)}} row4]))

    ;;(println rows)
    #_(for [[row1 row2] (take 2 rows)]
      (do
        (let [{:keys [id]} row1]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id id}} row1]))
        (let [{:keys [id]} row2]
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id id}} row2]))))


    #_(doseq [row-chunk (partition 10 rows)]
      (doseq [row row-chunk]
        (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row)}} row]))

      #_(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id 1}} (first rows)])
      #_(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id 2}} (second rows)]))
    #_(doseq [row-set (partition 2 rows)]
      (for [[row1 row2] row-set]
        (do
          (println row1)
          (println row2)
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row1)}} row1])
          (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row2)}} row2]))
        ))
    #_(for [[row1 row2 row3 row4] (partition 4 rows)]
      (do
        (async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row1)}} row1])
        ;(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row2)}} row2])
        ;(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row3)}} row3])
        ;(async/put! input-ch [{:index {:_index :booking :_type :_doc :_id (:id row4)}} row4])
        ))
    (future (loop [] (async/<!! output-ch)))))

(defn -main []
  (let [bookings (get-travel-bookings)]
    #_(doseq [booking bookings]
        (add-to-index booking))
    (add-to-index-bulk-async bookings)))
