# finite-capacity-load

Consumes the infinite capacity load calculated by IFS's CRP process and
produces finite capacity load data from it.

There are currently three tables to which data is output:

* `finite_load` a complete listing the calculated finite load, giving
  details per scheduled job, per day.
* `finite_load_sum` summarised load per work center and work day with
  one column per CRP source.
* `free_capacity` the inversion of the `load_sum` giving the periods
  when work centers are _not_ loaded. Each period is specified as a date
  range (`start_work_day` and `finish_work_day`) and the capacity
  available.

## Usage

FIXME

## License

Copyright © 2014 Lymington Precision Engineers Co. Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
