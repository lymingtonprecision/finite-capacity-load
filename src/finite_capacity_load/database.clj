(ns finite-capacity-load.database
  (:require [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as hk]))

(def default-options
  (merge
    hk/default-datasource-options
    {:auto-commit false
     :read-only false
     :minimum-idle 2
     :maximum-pool-size 10
     :adapter "oracle"
     :driver-type "thin"
     :port-number 1521}))

(def env-keys-to-option-names
  {:db-name :database-name
   :db-server :server-name
   :db-user :username})

(defn is-env-key? [k] (re-find #"(?i)^:?db-" (str k)))

(defn strip-env-key [k]
  (-> (str k)
      (clojure.string/replace #"(?i)^:?db-" "")
      keyword))

(defn extract-options-from-env [env]
  (->> env
       (filter #(is-env-key? (first %)))
       (reduce
         (fn [opts [k v]]
           (let [o (or (k env-keys-to-option-names)
                       (strip-env-key k))]
             (assoc opts o v)))
         {})))

(defn merge-options-with-env [env]
  (merge default-options (extract-options-from-env env)))

(defrecord Database [env]
  component/Lifecycle
  (start [this]
    (let [o (merge-options-with-env env)]
      (assoc this
             :options o
             :datasource (hk/make-datasource o))))

  (stop [this]
    (if-let [ds (:datasource this)]
      (hk/close-datasource ds))
    (dissoc this :datasource)))

(defn database []
  (component/using
    (map->Database {})
    [:env]))
