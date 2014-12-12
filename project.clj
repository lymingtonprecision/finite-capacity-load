(defproject finite-capacity-load "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]

                 [com.stuartsierra/component "0.2.2"]
                 [environ "1.0.0"]

                 [org.spootnik/logconfig "0.7.2"]

                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                 [hikari-cp "0.12.0"]
                 [yesql "0.4.0"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]}})
