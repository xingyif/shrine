/* Audit db tables in adapterAuditDB */
create table "queriesReceived" ("shrineNodeId" VARCHAR2(256) NOT NULL,"userName" VARCHAR2(256) NOT NULL,"networkQueryId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"topicId" VARCHAR2(256),"topicName" VARCHAR2(256),"timeQuerySent" NUMBER NOT NULL,"timeReceived" NUMBER NOT NULL);
create table "executionsStarted" ("networkQueryId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeExecutionStarted" NUMBER NOT NULL);
create table "executionsCompleted" ("networkQueryId" NUMBER NOT NULL,"replyId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeExecutionCompleted" NUMBER NOT NULL);
create table "resultsSent" ("networkQueryId" NUMBER NOT NULL,"replyId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeResultsSent" NUMBER NOT NULL);