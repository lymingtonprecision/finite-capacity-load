select
  wc.work_center_no,
  wtc.work_day,
  sum(
    nvl(
      wtc.working_time
      * (ra.efficiency / 100)
      * (wc.utilization / 100),
      0
    ) / 60
    - nvl(
      ifsapp.work_center_int_api.work_center_maint_load(
        wtc.work_day, wc.contract, wc.work_center_no
      ),
      0
    )
  ) hours_available
from ifsapp.work_center wc
join ifsapp.work_time_counter wtc
  on wc.calendar_id = wtc.calendar_id
left outer join ifsapp.work_center_resource_avail ra
  on wc.contract = ra.contract
  and wc.work_center_no = ra.work_center_no
  and ra.operable_db = 'Y'
  and wtc.work_day between
    ra.begin_date and
    nvl(ra.end_date, to_date('9999-12-31', 'yyyy-mm-dd'))
where wc.work_center_no = :work_center_no
  and wtc.work_day >= trunc(sysdate)
group by
  wc.contract,
  wc.work_center_no,
  wtc.work_day
order by
  wc.work_center_no,
  wtc.work_day
