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
  (let [c (wc/capacity-per-day wc {:connection db})
        l (wc/infinite-load wc {:connection db})
        s (into [] (fs/finite-scheduler c) l)
        f (into [] fc/free-capacity-accumulator s)]
    [wc s f]))

(defn <processor
  "Given a database connection (pool, preferably) and a queue of work center
  records starts a loop that will take a work center from the queue, process
  it as a single transaction, and repeat until the queue is empty"
  [db <wcq >r]
  (async/go
    (jdbc/with-db-connection [c db]
      (loop
        []
        (if-let [wc (async/<! <wcq)]
          (do
            (async/>! >r (process-work-center c wc))
            (recur)))))))

(defn <result-writer [db <r]
  (async/go
    (let  [c (jdbc/get-connection db)
           db {:connection {:connection c}}
           ac (.getAutoCommit c)]
      (.setAutoCommit c false)
      (try
        (loop
          []
          (if-let [[wc s ft] (async/<! <r)]
            (do
              (drop-work-center! db wc)
              (doseq [e s] (process-schedule-entry! db e))
              (doseq [e ft] (process-free-capacity-entry! db e))
              (recur))
            (do
              (.commit c))))
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
        <>r (async/chan 1000)
        _ (log/info "scheduling started with" n "workers," (/ n 2) "writers")
        <l (async/merge (map (fn [_] (<processor db <wcs <>r)) (range n)))
        <w (async/merge (map (fn [_] (<result-writer db <>r)) (range (/ n 2))))]
    (async/go-loop
      []
      (if-let [s (async/<! <l)]
        (recur)
        (do
          (log/info "processing finished, waiting on writers")
          (async/close! <>r))))
    (async/go-loop
      []
      (if-let [s (async/<! <w)]
        (recur)
        (log/info "writing finished")))))
