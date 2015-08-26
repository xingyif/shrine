alter table SHRINE_QUERY add flagged boolean not null default 0;
alter table SHRINE_QUERY add has_been_run boolean not null default 0;

alter table SHRINE_QUERY add index ix_SHRINE_QUERY_network_id (network_id);
alter table SHRINE_QUERY add index ix_SHRINE_QUERY_local_id (local_id);

-- mark all queries fom before now as having been run
update SHRINE_QUERY set has_been_run = 1;

alter table SHRINE_QUERY add flag_message varchar(255) null;
