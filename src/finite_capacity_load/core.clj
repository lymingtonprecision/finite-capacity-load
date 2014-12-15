(ns finite-capacity-load.core
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [finite-capacity-load.work-center :as wc]
            [finite-capacity-load.finite-schedule :as fs]
            [finite-capacity-load.finite-load :as fl]
            [finite-capacity-load.finite-load-sum :as fls]))

(def default-num-schedulers 10)
(def default-num-processors 4)

(defn blank-slate! [db]
  (fl/drop! {} db)
  (fls/drop! {} db))

(defn process-schedule-entry! [db s]
  (fl/insert-finite-loads! db s)
  (fls/insert-finite-load-sum! db s))

(defn <schedule-processor! [db <fs & [n]]
  (async/merge
    (map
      (fn [_]
        (async/go-loop
          []
          (if-let [s (async/<! <fs)]
            (do
              (process-schedule-entry! db s)
              (recur))
            :done)))
      (range (or n 1)))))

(defn generate-new-finite-schedule! [system]
  (let [db {:connection (:db system)}
        n (:workers (:env system) default-num-processors)
        ;--
        wcs (wc/active-work-centers {} db)
        cfn #(wc/capacity-per-day {:work_center_no %} db)
        lfn #(wc/infinite-load {:work_center_no %} db)
        ;--
        _ (blank-slate! db)
        _ (log/info "scheduling started")
        <fs (fs/<finite-schedule wcs default-num-schedulers cfn lfn)
        <l (<schedule-processor! db <fs n)]
    (loop
      []
      (if-let [s (async/<!! <l)]
        (recur)))
    (log/info "scheduling complete")))
