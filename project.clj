(defproject job-es-bookings "1.0.0-SNAPSHOT"
  :description "Populate elasticsearch from bookings"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.6.532"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [mysql/mysql-connector-java "8.0.18"]
                 [yesql "0.5.3"]
                 [cc.qbits/spandex "0.7.3"]]
  :main es-bookings.core
  :aot [es-bookings.core]
  :min-lein-version "2.7.0"
  :source-paths ["src/clj" "src/sql"])
