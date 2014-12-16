(ns finite-capacity-load.main
  (:require [finite-capacity-load.core :refer [generate-new-finite-schedule!]]
            [finite-capacity-load.system :as sys])
  (:gen-class))

(defn -main [& args]
  (let [s (.start (sys/system))]
    (generate-new-finite-schedule! s)))
