alter table SHRINE_QUERY add (temp CLOB);
update SHRINE_QUERY set temp=flag_message, flag_message=null;
alter table SHRINE_QUERY drop column flag_message;
alter table SHRINE_QUERY rename column temp to flag_message;
