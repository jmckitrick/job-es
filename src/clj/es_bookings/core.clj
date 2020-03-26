(ns es-bookings.core
  (:gen-class)
  (:require
   [clojure.string :as string]
   [clojure.core.async :as async]
   [qbits.spandex :as elastic-search]
   [yesql.core :refer [defqueries]]))

(def debug true)

(defn get-db-spec
  "Get a database connection spec.
  Prefer env variables then fall back to defaults."
  []
  (let [hostname (or (System/getenv "DB_HOST") "nodes.nonprod.kube.tstllc.net")
        username (or (System/getenv "DB_USER") "root")
        password (or (System/getenv "DB_PASS") "kube-aws")
        port (or (System/getenv "DB_PORT") 32114)]
    (when debug (println "Using" hostname "as db host."))
    {:dbtype "mysql"
     :dbname "book"
     :user username
     :password password
     :host hostname
     :port port
     :ssl false}))

;; Create clojure functions from these SQL queries.
(defqueries "travel-booking.sql" {:connection (get-db-spec)})

(defn elastic-search-config
  "Get settings for elastic search connection."
  []
  (let [host (or (System/getenv "ES_HOST") "localhost")
        port (or (System/getenv "ES_PORT") 8000)]
    {:host host
     :port port}))

(defn connect-to-elastic-search
  "Connect to an elastic search node.
  This may be a direct connection or a local connection via forwarded port."
  []
  (let [{:keys [host port]} (elastic-search-config)]
    (when debug (println "ElasticSearch host" host "port" port))
    (elastic-search/client {:hosts [(str "http://" host ":" port)]})))

(defn elastic-search-version
  [client]
  (let [result (elastic-search/request client {})
        version-string (get-in result [:body :version :number])
        version-pieces (string/split version-string #"\.")
        version-numbers (map read-string version-pieces)]
    (first version-numbers)))

(comment
  (def lower-es-client (connect-to-elastic-search))
  (elastic-search/request lower-es-client {})
  (elastic-search-version lower-es-client)
  )

(defn get-booking-type
  "Use attributes of BOOKING to determine additional
  booking type information."
  [booking]
  (cond
    (and (empty? (:subsite booking))
         (not-empty (:agent booking))) "agent"
    (and (empty? (:agent booking))
         (not-empty (:subsite booking))) "agent site"
    :else "web"))

(defn build-index-command
  "Create the command to index ROW for this version of ElasticSearch."
  [index-name row es-version]
  (let [base-command {:_index index-name
                      :_id (:id row)}]
    (case es-version
      6 (assoc base-command :_type :_doc)
      7 base-command)))

(defn add-to-index-bulk-async
  "Import ROWS into ElasticSearch index for ENV.
  Uses the bulk import API to import chunks of data.
  Return a channel to retrieve results."
  [rows env]
  (let [index-name (str "booking-" env)
        client (connect-to-elastic-search)
        es-version (elastic-search-version client)
        ;; This library builds a bulk channel to handle
        ;; many requests concurrently/asynchronously.
        ;; Data are inserted into INPUT-CH and results
        ;; are extracted from OUTPUT-CH.
        {:keys [input-ch output-ch]} (elastic-search/bulk-chan
                                      client
                                      {:flush-threshold 100
                                       :flush-interval 5000
                                       :max-concurrent-requests 1024})]
    (when debug
      (println "ES" es-version "index-name" index-name))
    (doseq [row-set (partition-all 1000 rows)]
      (doseq [row row-set]
        (let [ready-row (assoc row
                               :booking_type (get-booking-type row))
              index-command (build-index-command index-name ready-row es-version)]
          (async/>!! input-ch [{:index index-command} ready-row]))))
    (println "Imported all data in batch. Waiting on future.")
    output-ch))

(defn handle-bulk-results
  "Get the results from OUTPUT-CH asynchonously
  and return a future."
  [rows output-ch]
  (future
    (loop [n 0]
      (if (> n 0)
        (println "N:" n))
      (if (= n (count rows))
        n
        (let [[job responses] (async/<!! output-ch)]
          #_(prn "Job:" job)
          #_(prn "Responses:" responses)
          (when (instance? clojure.lang.ExceptionInfo responses)
            (println "Error during import.")
            (prn "Responses:" responses))
          (recur (+ n (count job))))))))

(comment
  (def my-bookings (get-travel-bookings {:start_date "2019-02-01 00:00:00" :end_date "2019-02-02 00:00:00"}))
  (def my-booking (get-travel-bookings {:start_date "2019-02-01 00:00:00" :end_date "2019-02-01 11:00:00"}))
  ;;(def my-import (import-bookings-impl "cdev" "2019-02-01 00:00:00" "2019-02-02 00:00:00"))
  (def my-import-count (add-to-index-bulk-async my-bookings "jcm"))
  (def my-import-count (add-to-index-bulk-async my-booking "jcm"))
  )

(defn import-bookings-impl
  "Get bookings for ENV between START-DATE and END-DATE.
  Send them to ElasticSearch via an async bulk channel."
  [env start-date end-date]
  (let [bookings (get-travel-bookings {:start_date start-date
                                       :end_date end-date})]
    (println "Env" env "Start" start-date "End" end-date)
    (println "Bookings:" (count bookings))
    (when (and (> (count bookings) 50000) debug)
      (println "----> OVER 50000!"))
    #_(println "Bookings: " bookings)
    (let [output-ch (add-to-index-bulk-async bookings env)
          result (handle-bulk-results bookings output-ch)]
      (println "Imported" @result "records.")
      #_(println "Exiting")
      #_(System/exit 0))))

(defn import-bookings
  "Build the dates and import bookings for ENV and YEAR."
  ([env year]
   (let [start-date (str year "-01-01 00:00:00")
         end-date (str (inc (read-string year)) "-01-01 00:00:00")]
     (import-bookings-impl env start-date end-date)))
  ([env year month]
   (let [start-date (str year "-" month "-01 00:00:00")
         end-date (str year "-" (inc (min (read-string month) 12)) "-01 00:00:00")]
     (import-bookings-impl env start-date end-date))))

(defn -main
  "Entry point to import bookings into elastic search.
  Expects environment name, year, and optional month to import."
  [& args]
  (when debug (println "Args" args))
  (when (seq args)
    (let [[env year month] args]
      (if month
        (import-bookings env year month)
        (import-bookings env year)))
    (System/exit 0)))
