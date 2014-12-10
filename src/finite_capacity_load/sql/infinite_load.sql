select
  l.work_day,
  l.contract,
  l.part_no,
  'M' bom_type_db,
  l.routing_revision_no routing_revision,
  l.routing_alternative_no routing_alternative,
  l.crp_source_db,
  to_char(l.counter) order_no,
  null release_no,
  null sequence_no,
  l.operation_no,
  (l.op_finish_time - l.op_start_time) load_hours,
  3 rnk,
  rank() over (
    order by
      l.work_day,
      l.op_start_time,
      l.op_finish_time
  ) subrnk
from ifsapp.crp_mach_op_load l
where work_center_no = :work_center_no
--
union
--
select
  l.work_day,
  l.contract,
  so.part_no,
  so.order_code_db,
  so.routing_revision,
  so.routing_alternative,
  decode(so.objstate, 'Planned', 'PSO', 'RSO') crp_source_db,
  l.order_no,
  l.release_no,
  l.sequence_no,
  l.operation_no,
  l.hours_loaded load_hours,
  decode(so.objstate, 'Planned', 2, 1) rnk,
  rank() over (
    order by
      l.work_day,
      l.order_no,
      l.release_no,
      l.sequence_no,
      l.operation_no
  ) subrnk
from ifsapp.mach_operation_load l
join ifsapp.shop_ord so
  on l.order_no = so.order_no
  and l.release_no = so.release_no
  and l.sequence_no = so.sequence_no
where work_center_no = :work_center_no
--
order by
  work_day,
  rnk,
  subrnk
