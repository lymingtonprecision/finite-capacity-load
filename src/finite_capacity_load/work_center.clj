(ns finite-capacity-load.work-center
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defquery]]))

(defquery active-work-centers "finite_capacity_load/sql/active_work_centers.sql")
(defquery work-hours-per-day "finite_capacity_load/sql/work_hours_per_day.sql")
(defquery infinite-load "finite_capacity_load/sql/infinite_load.sql")
