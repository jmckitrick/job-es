(defproject job-es-bookings "1.0.0-SNAPSHOT"
  :description "Populate elasticsearch from bookings"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 ;;[org.clojure/core.incubator "0.1.4"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [mysql/mysql-connector-java "5.1.40"]
                 [yesql "0.5.3"]
                 ;;[com.draines/postal "2.0.3"]
                 [cc.qbits/spandex "0.7.0"]
                 [hiccup "1.0.5"]]
  :main es-bookings.core
  :aot [es-bookings.core]
  :min-lein-version "2.7.0"
  :source-paths ["src/clj" "src/sql"])
