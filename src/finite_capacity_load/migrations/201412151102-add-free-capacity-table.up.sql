create table free_capacity (
  contract varchar2(20) not null,
  work_center_no varchar2(5) not null,
  start_work_day date not null,
  finish_work_day date not null,
  --
  capacity_available number default 0 not null,
  --
  rowversion date default sysdate not null,
  --
  constraint free_capacity_pk primary key (
    contract,
    work_center_no,
    start_work_day,
    finish_work_day,
    capacity_available
  )
);
