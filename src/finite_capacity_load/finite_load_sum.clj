(ns finite-capacity-load.finite-load-sum
  (:require [clojure.core.async :as async]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defquery]]))

(defquery drop! "finite_capacity_load/sql/drop_finite_load_sums.sql")
(defquery drop-work-center! "finite_capacity_load/sql/drop_finite_load_sums_wc.sql")
(defquery insert! "finite_capacity_load/sql/insert_finite_load_sum.sql")

(def zero-load
  {:load_from_ms 0
   :load_from_mrp 0
   :load_from_man 0
   :load_from_inv 0
   :load_from_nld 0
   :load_from_mso 0
   :load_from_dop 0
   :load_from_pso 0
   :load_from_rso 0
   :load_from_psc 0
   :load_from_pmrp 0})

(defn load-source-key [j]
  (->> (:crp_source_db j)
       (str "load_from_")
       clojure.string/lower-case
       keyword))

(defn sum-loads [s]
  (let [wc (dissoc s :load)
        l (reduce
            (fn [s j]
              (update s (load-source-key j)
                        (fnil + 0) (:scheduled_duration j)))
            {}
            (:load s))]
    (merge wc zero-load l)))

(defn insert-finite-load-sum! [db s]
  (if (pos? (:capacity_consumed s))
    (insert! (sum-loads s) db)))
