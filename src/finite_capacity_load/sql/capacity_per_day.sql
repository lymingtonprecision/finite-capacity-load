select
  wc.contract,
  wc.work_center_no,
  wtc.work_day,
  ifsapp.work_time_calendar_api.get_next_work_day(
    wc.calendar_id,
    wtc.work_day
  ) next_work_day,
  round(sum(
    nvl(
      wtc.working_time
      * (ra.efficiency / 100)
      * (wc.utilization / 100),
      0
    )
  ), 0) capacity,
  round(sum((
    select
      nvl(
        wtc.working_time
        * (ra.efficiency / 100)
        * (wc.utilization / 100),
        0
      )
      * least(count(*), 1)
    from ifsapp.active_work_order awo
    join ifsapp.operational_status os
      on awo.op_status_id = os.op_status_id
    where wcr.contract = awo.contract
      and wcr.mch_code = awo.mch_code
      and awo.objstate in ('UNDERPREPARATION','PREPARED','STARTED','RELEASED')
      and os.operational_status_type_db = '1'
      and wtc.work_day between awo.plan_s_date and awo.plan_f_date
  )), 0) maintenance,
  greatest(
    0,
    (
      round(sum(
        nvl(
          wtc.working_time
          * (ra.efficiency / 100)
          * (wc.utilization / 100),
          0
        )
      ), 0)
      -
      round(sum((
        select
          nvl(
            wtc.working_time
            * (ra.efficiency / 100)
            * (wc.utilization / 100),
            0
          )
          * least(count(*), 1)
        from ifsapp.active_work_order awo
        join ifsapp.operational_status os
          on awo.op_status_id = os.op_status_id
        where wcr.contract = awo.contract
          and wcr.mch_code = awo.mch_code
          and awo.objstate in ('UNDERPREPARATION','PREPARED','STARTED','RELEASED')
          and os.operational_status_type_db = '1'
          and wtc.work_day between awo.plan_s_date and awo.plan_f_date
      )), 0)
    )
  ) capacity_available
from ifsapp.work_center wc
--
join ifsapp.work_time_counter wtc
  on wc.calendar_id = wtc.calendar_id
--
join ifsapp.work_center_resource wcr
  on wc.contract = wcr.contract
  and wc.work_center_no = wcr.work_center_no
left outer join ifsapp.work_center_resource_avail ra
  on wcr.contract = ra.contract
  and wcr.work_center_no = ra.work_center_no
  and wcr.resource_id = ra.resource_id
  and ra.operable_db = 'Y'
  and wtc.work_day between
    ra.begin_date and
    nvl(ra.end_date, to_date('9999-12-31', 'yyyy-mm-dd'))
--
where wc.work_center_no = :work_center_no
  and wtc.work_day >= trunc(sysdate)
group by
  wc.contract,
  wc.work_center_no,
  wc.calendar_id,
  wtc.work_day
order by
  wtc.work_day
