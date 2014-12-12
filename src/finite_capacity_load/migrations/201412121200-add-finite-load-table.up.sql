create table finite_load (
  contract varchar2(20) not null,
  part_no varchar2(25) not null,
  bom_type_db varchar2(3) not null,
  routing_revision_no varchar2(4) not null,
  routing_alternative_no varchar2(20) not null,
  --
  crp_source_db varchar2(10) not null,
  order_ref varchar2(20) not null,
  order_no varchar2(12) not null,
  release_no varchar2(4),
  sequence_no varchar2(4),
  operation_no number not null,
  work_center_no varchar2(5) not null,
  --
  expected_start_date date not null,
  total_duration number not null,
  --
  work_day date not null,
  scheduled_duration number not null,
  --
  rowversion date default sysdate not null,
  --
  constraint finite_load_pk primary key (
    contract,
    part_no,
    crp_source_db,
    order_ref,
    operation_no,
    work_day
  )
);

create index finite_load_ix1 on finite_load (
  contract,
  work_day,
  crp_source_db
);

create index finite_load_ix2 on finite_load (
  contract,
  order_no,
  release_no,
  sequence_no,
  operation_no
);
