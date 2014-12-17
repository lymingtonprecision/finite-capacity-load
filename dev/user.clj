(ns user
  (:require [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [com.stuartsierra.component :as component]
            [environ.core :refer (env)]

            [finite-capacity-load.system :as sys]

            [clojure.java.jdbc :as jdbc]
            [finite-capacity-load.database :as db]

            [finite-capacity-load.core]
            [finite-capacity-load.work-center]
            [finite-capacity-load.finite-load]
            [finite-capacity-load.finite-load-sum]
            [finite-capacity-load.finite-schedule]
            [finite-capacity-load.free-capacity]))

(in-ns 'environ.core)

(defn refresh-env
  "A hack to allow in-repl refresh of the environment vars"
  []
  (def env
    (merge (read-env-file)
           (read-system-env)
           (read-system-props))))

(in-ns 'user)

(def system nil)

(defn init []
  (environ.core/refresh-env)
  (alter-var-root #'system (constantly (sys/system))))

(defn start []
  (alter-var-root #'system sys/start))

(defn stop []
  (if system
    (alter-var-root #'system sys/stop)))

(defn go
  "Initialize the current development system and start it's components"
  []
  (init)
  (start)
  :running)

(defn reset []
  (stop)
  (refresh :after 'user/go))
