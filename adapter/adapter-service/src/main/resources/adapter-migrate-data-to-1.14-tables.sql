alter table REQUEST_RESPONSE_DATA add index `IDX_REQUEST_RESPONSE_DATA_BROADCAST_QUERY_MASTER_ID`(BROADCAST_QUERY_MASTER_ID);
alter table SHRINE_QUERY add index `IDX_SHRINE_QUERY_NETWORK_ID`(network_id);
alter table RESULT_IDS add index `IDX_RESULT_IDS_BROADCAST_RESULT_INSTANCE_ID`(BROADCAST_RESULT_INSTANCE_ID);

insert into 
    SHRINE_QUERY (local_id, network_id, username, domain, query_name, query_expression, date_created) 
(select 
    mq.LOCAL_QUERY_MASTER_ID, mq.BROADCAST_QUERY_MASTER_ID, USERNAME, DOMAIN_NAME, MASTER_NAME, QUERY_DEFINITION, TIMESTAMP(utmq.MASTER_CREATE_DATE)
 from 
    USERS_TO_MASTER_QUERY utmq inner join 
    MASTER_QUERY mq on utmq.BROADCAST_QUERY_MASTER_ID = mq.BROADCAST_QUERY_MASTER_ID);

insert into 
    QUERY_RESULT (local_id, query_id, type, status, time_elapsed, last_updated) 
(select 
    rids.LOCAL_RESULT_INSTANCE_ID, sq.id, 'PATIENT_COUNT_XML', 'FINISHED', rrd.TIME_ELAPSED, sq.date_created 
 from 
    REQUEST_RESPONSE_DATA rrd inner join 
    RESULT_IDS rids on rrd.BROADCAST_RESULT_INSTANCE_ID = rids.BROADCAST_RESULT_INSTANCE_ID inner join 
    SHRINE_QUERY sq on rrd.BROADCAST_QUERY_MASTER_ID = sq.network_id);
    
insert into 
    COUNT_RESULT (result_id, original_count, obfuscated_count, date_created) 
(select 
    qr.id, rrd.RESULT_SET_SIZE, rrd.RESULT_SET_SIZE + rids.OBFUSCATION_AMOUNT, sq.date_created 
 from 
    SHRINE_QUERY sq inner join 
    QUERY_RESULT qr on sq.id = qr.query_id inner join 
    REQUEST_RESPONSE_DATA rrd on rrd.BROADCAST_QUERY_MASTER_ID = sq.network_id inner join 
    RESULT_IDS rids on rrd.BROADCAST_RESULT_INSTANCE_ID = rids.BROADCAST_RESULT_INSTANCE_ID);
