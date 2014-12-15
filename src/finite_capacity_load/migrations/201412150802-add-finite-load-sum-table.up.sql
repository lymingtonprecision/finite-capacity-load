create table finite_load_sum (
  contract varchar2(20) not null,
  work_center_no varchar2(5) not null,
  work_day date not null,
  --
  capacity number not null,
  maintenance number not null,
  capacity_available number not null,
  capacity_consumed number not null,
  --
  load_from_ms number default 0 not null,
  load_from_mrp number default 0 not null,
  load_from_man number default 0 not null,
  load_from_inv number default 0 not null,
  load_from_nld number default 0 not null,
  load_from_mso number default 0 not null,
  load_from_dop number default 0 not null,
  load_from_pso number default 0 not null,
  load_from_rso number default 0 not null,
  load_from_psc number default 0 not null,
  load_from_pmrp number default 0 not null,
  --
  rowversion date default sysdate not null,
  --
  constraint finite_load_sum_pk primary key (
    contract,
    work_center_no,
    work_day
  )
);
