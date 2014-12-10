select
  wc.contract,
  wc.work_center_no,
  wc.average_capacity
from ifsapp.work_center wc
where ifsapp.work_center_resource_avail_api.get_default_resource_id(
    wc.contract,
    wc.work_center_no,
    sysdate
  ) is not null
