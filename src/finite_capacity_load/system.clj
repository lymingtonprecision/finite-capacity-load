(ns finite-capacity-load.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer (env)]
            [finite-capacity-load.logging :as log]
            [finite-capacity-load.database :as db]))

(defn system
  ([] (system env))
  ([env]
   (component/system-map
     :env env
     :db (db/database))))

(defn start [s]
  (log/start!)
  (component/start s))

(defn stop [s]
  (log/stop!)
  (component/stop s))
