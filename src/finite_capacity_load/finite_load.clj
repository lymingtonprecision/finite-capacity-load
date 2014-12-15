(ns finite-capacity-load.finite-load
  (:require [clojure.core.async :as async]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defquery]]))

(defquery drop! "finite_capacity_load/sql/drop_finite_load.sql")
(defquery insert! "finite_capacity_load/sql/insert_finite_load.sql")

(defn order-ref [o]
  (reduce
    (fn [r k]
      (if-let [v (k o)]
        (if (empty? r) v (str r "-" v))
        r))
    ""
    [:order_no :release_no :sequence_no]))

(defn finite-schedule->finite-load [s]
  (map
    (fn [l]
      (merge (assoc l :order_ref (order-ref l))
             (select-keys s [:work_center_no])))
    (:load s)))

(defn insert-finite-loads! [db s]
  (doseq [l (finite-schedule->finite-load s)]
    (insert! l db)))
