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

First, set the following environment variables:

    DB_NAME=the name of the Oracle instance to connect to
    DB_SERVER=the name/address of the Oracle server
    DB_USER=the user to connect as
    DB_PASSWORD=the password to connect with

(Note: [environ](https://github.com/weavejester/environ) is being used
so these could be entered in `.lein-env` file.)

Then execute the `jar` file:

    java -jar <path\to\finite-capacity-load.jar>

### Requirements

In order to run successfully the user account used must have the
following access rights:

    grant create session to user;
    grant create table to user;
    -- selects
    grant select on ifsapp.crp_mach_op_load to user;
    grant select on ifsapp.mach_operation_load to user;
    grant select on ifsapp.shop_ord to user;
    grant select on ifsapp.work_center to user;
    grant select on ifsapp.work_center_resource_avail to user;
    grant select on ifsapp.work_time_counter to user;
    -- apis
    grant execute on ifsapp.work_time_calendar_api to user;
    grant execute on ifsapp.work_center_int_api to user;
    grant execute on ifsapp.work_center_resource_avail_api to user;

As the user will be creating tables they will also need a suitable quota
on the tablespace (use `alter user <username> quota unlimited on
<tablespace>;` in a pinch.)

## License

Copyright © 2014 Lymington Precision Engineers Co. Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
