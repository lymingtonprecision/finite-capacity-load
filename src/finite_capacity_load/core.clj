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

(defn process-work-center [db wc]
  (let [c (wc/capacity-per-day wc db)
        l (wc/infinite-load wc db)
        s (into [] (fs/finite-scheduler c) l)
        f (into [] fc/free-capacity-accumulator s)]
    (drop-work-center! db wc)
    (doseq [e s] (process-schedule-entry! db e))
    (doseq [e f] (process-free-capacity-entry! db e))))

(defn <processor
  "Given a database connection (pool, preferably) and a queue of work center
  records starts a loop that will take a work center from the queue, process
  it as a single transaction, and repeat until the queue is empty"
  [db <wcq]
  (async/go
    (let [c (jdbc/get-connection db)
          db {:connection {:connection c}}
          ac (.getAutoCommit c)]
      (.setAutoCommit c false)
      (try
        (loop
          []
          (if-let [wc (async/<! <wcq)]
            (do
              (process-work-center db wc)
              (recur))
            (.commit c)))
        (catch Throwable t
          (.rollback c)
          (throw t))
        (finally
          (.setAutoCommit c ac)
          (.close c))))))

(defn generate-new-finite-schedule! [system]
  (let [db (select-keys (:db system) [:datasource])
        n (:workers (:env system) default-num-processors)
        <wcs (jdbc/with-db-connection [c db]
               (async/to-chan (wc/active-work-centers {} {:connection c})))
        _ (log/info "scheduling started with" n "workers")
        <l (async/merge (map (fn [_] (<processor db <wcs)) (range n)))]
    (async/go-loop
      []
      (if-let [s (async/<! <l)]
        (recur)
        (log/info "scheduling complete")))))
