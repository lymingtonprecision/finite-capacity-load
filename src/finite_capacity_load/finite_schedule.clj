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

(defn remaining-capacity [d]
  (if-let [a (:capacity_available d)]
    (max 0 (- a (get d :capacity_consumed 0)))))

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
        l (conj (get d :load []) lo)]
    (assoc d :capacity_consumed ch :load l)))

(defn schedule-order
  "Finite schedules `order` using the provided available `capacity`,
  returning the updated entries from `capacity` as a tuple of fully
  consumed days and partially/fully unconsumed days.

  `capacity` should be a sequence of maps with `:work_day` and
  `:capacity_available` entries.

  `order` should be a map with `:start_date` and `:duration` entries."
  [capacity order]
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
      [(:duration order) [] (subseq capacity < {:work_day (:start_date order)})]
      (subseq capacity >= {:work_day (:start_date order)}))))

(defn finite-scheduler
  "A finite scheduling transducer"
  [capacity]
  (fn [step]
    (let [remaining-capacity (volatile! (sorted-capacity capacity))]
      (fn
        ([] (step))
        ([r]
         (step (reduce step r
                       (map #(merge {:capacity_consumed 0 :load []} %)
                            @remaining-capacity))))
        ([r l]
         (let [[consumed unconsumed] (schedule-order @remaining-capacity l)]
           (vreset! remaining-capacity (sorted-capacity unconsumed))
           (if (seq consumed)
             (reduce step r consumed)
             r)))))))

(defn <finite-schedule
  "Produces a finite schedule for the list of work centers `wcs` returning a
  channel onto which the finite schedule entries are put. The returned channel
  will be closed when the schedule is complete.

  * `wcs` a list of work centers to schedule each entry should be a map with a
    `:work_center_no` entry
  * `s` the number of scheduler processes to create
  * `cfn` a `fn` to call with a work center number to retrieve it's capacity
  * `lfn` a `fn` to call with a work center number returning the orders to load
  * `buffer`, optional, the buffer to use on the returned channel, defaults to
    a fixed buffer of 100 entries"
  [wcs s cfn lfn & [buffer]]
  (let [<wc (async/to-chan wcs)
        <>fs (async/chan (or buffer 100))
        <m (async/merge
             (map
               (fn [_]
                 (async/go-loop
                   []
                   (if-let [{wc :work_center_no} (async/<! <wc)]
                     (let [s (finite-scheduler (cfn wc))
                           l (into [] s (lfn wc))]
                       (doseq [o l] (async/>! <>fs o))
                       (recur))
                     :done)))
               (range s)))]
    (async/go-loop
      []
      (if-let [n (async/<! <m)]
        (recur)
        (async/close! <>fs)))
    <>fs))
