(ns finite-capacity-load.core
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [finite-capacity-load.work-center :as wc]
            [finite-capacity-load.finite-schedule :as fs]
            [finite-capacity-load.finite-load :as fl]
            [finite-capacity-load.finite-load-sum :as fls]
            [finite-capacity-load.free-capacity :as fc]))

(def default-num-processors 10)

(defn connection [db] {:connection db})

(defn blank-slate! [db]
  (fl/drop! {} db)
  (fls/drop! {} db)
  (fc/drop! {} db))

(defn drop-work-center! [db wc]
  (fl/drop-work-center! wc db)
  (fls/drop-work-center! wc db)
  (fc/drop-work-center! wc db))

(defn process-schedule-entry! [db s]
  (fl/insert-finite-loads! db s)
  (fls/insert-finite-load-sum! db s))

(defn process-free-capacity-entry! [db fc]
  (fc/insert! fc db))

(defn <process-queue [<q f]
  (async/go-loop
    []
    (if-let [v (async/<! <q)]
      (do
        (f v)
        (recur)))))

(defn perform-work-center-transactions! [db wc s f]
  (let [<s (async/to-chan s)
        <f (async/to-chan f)]
    (jdbc/with-db-transaction [tx db]
      (drop-work-center! (connection tx) wc)
      (let [<p (async/merge
                 [(<process-queue
                    <s (partial process-schedule-entry! (connection tx)))
                  (<process-queue
                    <f (partial process-free-capacity-entry! (connection tx)))])]
        (loop
          []
          (if-let [_ (async/<!! <p)]
            (recur)))))))

(defn process-work-center [db wc]
  (let [c (wc/capacity-per-day wc (connection db))
        l (wc/infinite-load wc (connection db))
        s (into [] (fs/finite-scheduler c) l)
        f (into [] fc/free-capacity-accumulator s)]
    (perform-work-center-transactions! db wc s f)))

(defn <processor
  "Given a database connection (pool, preferably) and a queue of work center
  records starts a loop that will take a work center from the queue, process
  it as a single transaction, and repeat until the queue is empty"
  [db <wcq]
  (async/go
    (jdbc/with-db-connection [c db]
      (loop
        []
        (if-let [wc (async/<! <wcq)]
          (do
            (process-work-center c wc)
            (recur)))))))

(defn generate-new-finite-schedule! [system]
  (let [db (:db system)
        n (:workers (:env system) default-num-processors)
        <wcs (async/to-chan (wc/active-work-centers {} (connection db)))
        _ (log/info "scheduling started")
        <l (async/merge (map (fn [_] (<processor db <wcs)) (range n)))]
    (loop
      []
      (if-let [s (async/<!! <l)]
        (recur)))
    (log/info "scheduling complete")))
