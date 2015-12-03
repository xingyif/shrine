create table shrine_query(
  id int not null,
  local_id varchar(255) not null,
  network_id int not null,
  username varchar(255) not null,
  domain varchar(255) not null,
  query_definition CLOB not null,
  date_created timestamp default systimestamp,
  constraint query_id_pk primary key(id)
);

create sequence seq_shrine_query_id
minvalue 1
start with 1
increment by 1;

create table query_result(
  id int not null,
  local_id varchar(255) not null,
  query_id int not null,
  type varchar(255) not null check (type in ('PATIENTSET','PATIENT_COUNT_XML','PATIENT_AGE_COUNT_XML','PATIENT_RACE_COUNT_XML','PATIENT_VITALSTATUS_COUNT_XML','PATIENT_GENDER_COUNT_XML','ERROR')),
  status varchar(255) not null check (status in ('FINISHED', 'ERROR', 'PROCESSING', 'QUEUED')),
  time_elapsed int not null,
  last_updated timestamp default systimestamp,
  constraint query_result_id_pk primary key(id),
  constraint fk_query_result_query_id foreign key (query_id) references shrine_query (id)
);

create sequence seq_query_result_id
minvalue 1
start with 1
increment by 1;

create table error_result(
  id int not null,
  result_id int not null,
  message varchar(255) not null,
  constraint error_result_id_pk primary key(id),
  constraint fk_error_result_query_id foreign key (result_id) references query_result (id)
);
alter table ERROR_RESULT add column 'CODEC' varchar not null default "Pre-1.20 Error";
alter table ERROR_RESULT add column 'SUMMARY' text not null default "Pre-1.20 Error";
alter table ERROR_RESULT add column 'DESCRIPTION' text not null default "Pre-1.20 Error";
alter table ERROR_RESULT add column 'DETAILS' text not null default "Pre-1.20 Error";


create sequence seq_error_result_id
minvalue 1
start with 1
increment by 1;

create table count_result(
  id int not null,
  result_id int not null,
  original_count int not null,
  obfuscated_count int not null,
  date_created timestamp default systimestamp,
  constraint count_result_id_pk primary key(id),
  constraint fk_count_result_query_id foreign key (result_id) references query_result (id)
);

create sequence seq_count_result_id
minvalue 1
start with 1
increment by 1;

create table breakdown_result(
  id int not null,
  result_id int not null,
  data_key varchar(255) not null,
  original_value int not null,
  obfuscated_value int not null,
  constraint breakdown_result_id_pk primary key(id),
  constraint fk_bd_result_query_id foreign key (result_id) references query_result (id)
);

create sequence seq_breakdown_result_id
minvalue 1
start with 1
increment by 1;

create table patient_set(
  id int not null,
  result_id int not null,
  patient_num varchar(255) not null,
  constraint patient_set_id_pk primary key(id),
  constraint fk_ps_query_id foreign key (result_id) references query_result (id)
);

create sequence seq_patient_set_id
minvalue 1
start with 1
increment by 1;

create table privileged_user(
  id int not null,
  username varchar(255) not null,
  domain varchar(255) not null,
  threshold int not null,
  override_date timestamp null,
  constraint priviliged_user_pk primary key(id),
  constraint ix_pu_username_domain unique (username, domain)
);

create sequence seq_privileged_user_id
minvalue 1
start with 1
increment by 1;

create trigger shrine_query_insert_add_id before insert on shrine_query
for each row
begin
    select seq_shrine_query_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger query_result_insert_add_id before insert on query_result
for each row
begin
    select seq_query_result_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger error_result_insert_add_id before insert on error_result
for each row
begin
    select seq_error_result_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger count_result_insert_add_id before insert on count_result
for each row
begin
    select seq_count_result_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger breakdown_result_insert_add_id before insert on breakdown_result
for each row
begin
    select seq_breakdown_result_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger patient_set_insert_add_id before insert on patient_set
for each row
begin
    select seq_patient_set_id.NEXTVAL into :NEW.id from dual;
end;
/
create trigger privileged_user_insert_add_id before insert on privileged_user
for each row
begin
    select seq_privileged_user_id.NEXTVAL into :NEW.id from dual;
end;
/
