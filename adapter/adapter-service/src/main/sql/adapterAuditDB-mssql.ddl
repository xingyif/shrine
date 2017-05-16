/* Audit db tables in adapterAuditDB */
create table "queriesReceived" ("shrineNodeId" VARCHAR(MAX) NOT NULL,"userName" VARCHAR(MAX) NOT NULL,"networkQueryId" BIGINT NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"topicId" VARCHAR(MAX),"topicName" VARCHAR(MAX),"timeQuerySent" BIGINT NOT NULL,"timeReceived" BIGINT NOT NULL);
create table "executionsStarted" ("networkQueryId" BIGINT NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"timeExecutionStarted" BIGINT NOT NULL);
create table "executionsCompleted" ("networkQueryId" BIGINT NOT NULL,"replyId" BIGINT NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"timeExecutionCompleted" BIGINT NOT NULL);
create table "resultsSent" ("networkQueryId" BIGINT NOT NULL,"replyId" BIGINT NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"timeResultsSent" BIGINT NOT NULL);