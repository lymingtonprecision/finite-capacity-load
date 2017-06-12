select
  min(l.work_day) expected_start_date,
  l.contract,
  l.part_no,
  'M' bom_type_db,
  l.routing_revision_no routing_revision_no,
  l.routing_alternative_no routing_alternative_no,
  l.crp_source_db,
  to_char(l.counter) order_no,
  null release_no,
  null sequence_no,
  l.operation_no,
  round(sum(l.op_finish_time - l.op_start_time) * 60, 0) total_duration,
  3 rnk
from ifsapp.crp_mach_op_load l
where l.contract = :contract
  and l.work_center_no = :work_center_no
group by
  l.contract,
  l.part_no,
  l.routing_revision_no,
  l.routing_alternative_no,
  l.crp_source_db,
  l.counter,
  l.operation_no
--
union
--
select
  min(l.work_day) expected_start_date,
  l.contract,
  so.part_no,
  so.order_code_db,
  so.routing_revision routing_revision_no,
  so.routing_alternative routing_alternative_no,
  decode(so.objstate, 'Planned', 'PSO', 'RSO') crp_source_db,
  l.order_no,
  l.release_no,
  l.sequence_no,
  l.operation_no,
  round(sum(l.hours_loaded) * 60, 0) total_duration,
  decode(so.objstate, 'Planned', 2, 1) rnk
from ifsapp.mach_operation_load l
join ifsapp.shop_ord so
  on l.order_no = so.order_no
  and l.release_no = so.release_no
  and l.sequence_no = so.sequence_no
where l.contract = :contract
  and l.work_center_no = :work_center_no
group by
  l.contract,
  so.part_no,
  so.order_code_db,
  so.routing_revision,
  so.routing_alternative,
  so.objstate,
  l.order_no,
  l.release_no,
  l.sequence_no,
  l.operation_no
--
order by
  expected_start_date,
  rnk,
  order_no,
  release_no,
  sequence_no,
  operation_no
