(ns finite-capacity-load.free-capacity
  (:require [clojure.core.async :as async]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defquery]]))

(defquery drop! "finite_capacity_load/sql/drop_free_capacity.sql")
(defquery drop-work-center! "finite_capacity_load/sql/drop_free_capacity_wc.sql")
(defquery insert! "finite_capacity_load/sql/insert_free_capacity.sql")

(defn finite-schedule->free-capacity [s]
 (assoc (select-keys s [:contract :work_center_no])
        :start_work_day (:work_day s)
        :finish_work_day (:work_day s)
        :next_work_day (:next_work_day s)
        :capacity_available (:capacity_available s)))

(defn reset-free-capacity [fc wc wd]
  (assoc fc wc (finite-schedule->free-capacity wd)))

(defn update-free-capacity [fc wc wd]
  (let [wc-fc (-> (get fc wc (finite-schedule->free-capacity wd))
                  (assoc :finish_work_day (:work_day wd)
                         :next_work_day (:next_work_day wd))
                  (update :capacity_available + (:capacity_available wd)))]
    (assoc fc wc wc-fc)))

(defn free-capacity-accumulator [step]
  (let [fc (volatile! {})]
    (fn
      ([] (step))
      ([r]
       (step (reduce step r (vals @fc))))
      ([r wd]
       (if (> (:capacity_available wd) 0)
         (let [wc (:work_center_no wd)
               wc-fc (get @fc wc)
               nwd (:next_work_day wc-fc)]
           (if (and nwd (= (:work_day wd) (:next_work_day wc-fc)))
             (do
               (vswap! fc update-free-capacity wc wd)
               r)
             (do
               (vswap! fc reset-free-capacity wc wd)
               (if (nil? wc-fc) r (step r wc-fc)))))
         r)))))
