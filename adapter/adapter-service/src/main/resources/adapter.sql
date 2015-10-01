create table SHRINE_QUERY(
  id int not null auto_increment,
  local_id varchar(255) not null,
  network_id bigint not null,
  username varchar(255) not null,
  domain varchar(255) not null,
  query_name varchar(255) not null,
  query_expression text,
  date_created timestamp default current_timestamp,
  has_been_run boolean not null default 0,
  flagged boolean not null default 0,
  flag_message varchar(255) null,
  constraint query_id_pk primary key(id),
  index ix_SHRINE_QUERY_network_id (network_id),
  index ix_SHRINE_QUERY_local_id (local_id),
  index ix_SHRINE_QUERY_username_domain (username, domain),
  query_xml text
) engine=innodb default charset=latin1;

create table QUERY_RESULT(
  id int not null auto_increment,
  local_id varchar(255) not null,
  query_id int not null,
  type enum('PATIENTSET','PATIENT_COUNT_XML','PATIENT_AGE_COUNT_XML','PATIENT_RACE_COUNT_XML','PATIENT_VITALSTATUS_COUNT_XML','PATIENT_GENDER_COUNT_XML','ERROR') not null,
  status enum('FINISHED', 'ERROR', 'PROCESSING', 'QUEUED') not null,
  time_elapsed int null,
  last_updated timestamp default current_timestamp,
  constraint QUERY_RESULT_id_pk primary key(id),
  constraint fk_QUERY_RESULT_query_id foreign key (query_id) references SHRINE_QUERY (id) on delete cascade
) engine=innodb default charset=latin1;

create table ERROR_RESULT(
  id int not null auto_increment,
  result_id int not null,
  message varchar(255) not null,
  constraint ERROR_RESULT_id_pk primary key(id),
  constraint fk_ERROR_RESULT_QUERY_RESULT_id foreign key (result_id) references QUERY_RESULT (id) on delete cascade
) engine=innodb default charset=latin1;

alter table ERROR_RESULT add column 'CODEC' varchar not null default "Pre-1.20 Error"
alter table ERROR_RESULT add column 'SUMMARY' text not null default "Pre-1.20 Error"
alter table ERROR_RESULT add column 'DESCRIPTION' text not null default "Pre-1.20 Error"
alter table ERROR_RESULT add column 'DETAILS' text not null default "Pre-1.20 Error"

create table COUNT_RESULT(
  id int not null auto_increment,
  result_id int not null,
  original_count int not null,
  obfuscated_count int not null,
  date_created timestamp default current_timestamp,
  constraint COUNT_RESULT_id_pk primary key(id),
  constraint fk_COUNT_RESULT_QUERY_RESULT_id foreign key (result_id) references QUERY_RESULT (id) on delete cascade
) engine=innodb default charset=latin1;

create table BREAKDOWN_RESULT(
  id int not null auto_increment,
  result_id int not null,
  data_key varchar(255) not null,
  original_value int not null,
  obfuscated_value int not null,
  constraint BREAKDOWN_RESULT_id_pk primary key(id),
  constraint fk_BREAKDOWN_RESULT_QUERY_RESULT_id foreign key (result_id) references QUERY_RESULT (id) on delete cascade
) engine=innodb default charset=latin1;

create table PRIVILEGED_USER(
  id int not null auto_increment,
  username varchar(255) not null,
  domain varchar(255) not null,
  threshold int not null,
  override_date timestamp null,
  constraint priviliged_user_pk primary key(id),
  constraint ix_PRIVILEGED_USER_username_domain unique (username, domain)
) engine=innodb default charset=latin1;
