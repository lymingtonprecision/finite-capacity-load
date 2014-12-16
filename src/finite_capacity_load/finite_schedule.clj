(ns finite-capacity-load.finite-schedule
  (:require [clojure.core.async :refer (go) :as async]
            [clojure.tools.logging :as log]))

(defn sorted-capacity
  "Returns `capacity` as a `sorted-set` ordered the `:work_day`s of its entries"
  [capacity]
  (if (sorted? capacity)
    capacity
    (apply sorted-set-by
           (fn [x y] (compare (:work_day x) (:work_day y)))
           capacity)))

(defn merge-consumption-fields [m]
  (merge {:capacity_consumed 0 :load []} m))

(defn remaining-capacity [d]
  (max 0 (:capacity_available d 0)))

(defn update-load
  "Updates a work day to incorporate `n` units of load from order `o`

  The returned record will contain the additional entries:

  * `:capacity_consumed` the total capacity consumed by scheduled jobs
    on that day
  * `:load` a sequence of jobs scheduled on that day, each job has the
    additional keys:

    * `:work_day` the day the job has been scheduled
    * `:scheduled_duration` the duration for which the job has been
      scheduled"
  [d o n]
  (let [lo (assoc o :work_day (:work_day d) :scheduled_duration n)
        ch (+ n (get d :capacity_consumed 0))
        ac (max 0 (- (remaining-capacity d) ch))
        l (conj (get d :load []) lo)]
    (assoc d :capacity_available ac :capacity_consumed ch :load l)))

(defn schedule-order
  "Finite schedules `order` using the provided available `capacity`,
  returning the updated entries from `capacity` as a tuple of fully
  consumed days and partially/fully unconsumed days.

  `capacity` should be a sequence of maps with `:work_day` and
  `:capacity_available` entries.

  `order` should be a map with `:expected_start_date` and `:total_duration`
  entries."
  [capacity order]
  (let [d (:total_duration order)
        sd (:expected_start_date order)]
    (rest
      (reduce
        (fn [[rlh consumed unconsumed] d]
          (let [ch (min rlh (remaining-capacity d))
                rlh (- rlh ch)
                ld (if (zero? ch) d (update-load d order ch))
                rh (remaining-capacity ld)
                cd (if (zero? rh) (conj consumed ld) consumed)
                ucd (if (pos? rh) (conj unconsumed ld) unconsumed)]
            [rlh cd ucd]))
        [d [] (subseq capacity < {:work_day sd})]
        (subseq capacity >= {:work_day sd})))))

(defn finite-scheduler
  "A finite scheduling transducer

  Requires a sequence of `capacity` entries from which to work. Each
  entry in the sequence should be a map consisting of: `work_day` and
  `capacity_available` entries.

  Transduced items should be load entries with `expected_start_date` and
  `total_duration` keys.

  The resulting reductions will be entries from `capacity` with the
  following changes:

  * `load` a sequence of items that have been scheduled on that day by
    the transducer
  * `capacity_consumed` the total capacity consumed by the items in
    `load`
  * `capacity_available` the remaining, unconsumed, capacity"
  [capacity]
  (fn [step]
    (let [remaining-capacity (volatile! (sorted-capacity capacity))]
      (fn
        ([] (step))
        ([r]
         (step (reduce step r
                       (map merge-consumption-fields @remaining-capacity))))
        ([r l]
         (let [[consumed unconsumed] (schedule-order @remaining-capacity l)]
           (vreset! remaining-capacity (sorted-capacity unconsumed))
           (if (seq consumed)
             (reduce (fn [r l]
                       (->> l merge-consumption-fields (step r)))
                     r consumed)
             r)))))))
