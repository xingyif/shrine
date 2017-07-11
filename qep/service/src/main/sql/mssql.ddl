create database qepAuditDB;
use qepAuditDB;
create table "queriesSent" ("shrineNodeId" VARCHAR(MAX) NOT NULL,"userName" VARCHAR(MAX) NOT NULL,"networkQueryId" BIGINT NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"queryTopicId" VARCHAR(MAX),"queryTopicName" VARCHAR(MAX),"timeQuerySent" BIGINT NOT NULL);
create table "previousQueries" ("networkId" BIGINT NOT NULL,"userName" VARCHAR(MAX) NOT NULL,"domain" VARCHAR(MAX) NOT NULL,"queryName" VARCHAR(MAX) NOT NULL,"expression" VARCHAR(MAX),"dateCreated" BIGINT NOT NULL,"deleted" BIT NOT NULL,"queryXml" VARCHAR(MAX) NOT NULL,"changeDate" BIGINT NOT NULL);
create table "queryFlags" ("networkId" BIGINT NOT NULL,"flagged" BIT NOT NULL,"flagMessage" VARCHAR(MAX) NOT NULL,"changeDate" BIGINT NOT NULL);
create table "queryResults" ("resultId" BIGINT NOT NULL,"networkQueryId" BIGINT NOT NULL,"instanceId" BIGINT NOT NULL,"adapterNode" VARCHAR(MAX) NOT NULL,"resultType" VARCHAR(MAX),"size" BIGINT NOT NULL,"startDate" BIGINT,"endDate" BIGINT,"status" VARCHAR(MAX) NOT NULL,"statusMessage" VARCHAR(MAX),"changeDate" BIGINT NOT NULL);
create table "queryBreakdownResults" ("networkQueryId" BIGINT NOT NULL,"adapterNode" VARCHAR(MAX) NOT NULL,"resultId" BIGINT NOT NULL,"resultType" VARCHAR(MAX) NOT NULL,"dataKey" VARCHAR(MAX) NOT NULL,"value" BIGINT NOT NULL,"changeDate" BIGINT NOT NULL);
create table "queryResultProblemDigests" ("networkQueryId" BIGINT NOT NULL,"adapterNode" VARCHAR(MAX) NOT NULL,"codec" VARCHAR(MAX) NOT NULL,"stamp" VARCHAR(MAX) NOT NULL,"summary" VARCHAR(MAX) NOT NULL,"description" VARCHAR(MAX) NOT NULL,"details" VARCHAR(MAX) NOT NULL,"changeDate" BIGINT NOT NULL);

create index "queryResultsChangeDateIndex" on "queryResults" ("changeDate");
create index "queryResultsNetworkQueryIdIndex" on "queryResults" ("networkQueryId");
-- create index "queryResultsAdapterNodeIndex" on "queryResults" ("adapterNode");

create index "queryBreakdownResultsChangeDateIndex" on "queryBreakdownResults" ("changeDate");
create index "queryBreakdownResultsNetworkQueryIdIndex" on "queryBreakdownResults" ("networkQueryId");
-- create index "queryBreakdownResultsAdapterNodeIndex" on "queryBreakdownResults" ("adapterNode");

create index "queryFlagsChangeDateIndex" on "queryFlags" ("changeDate");
create index "queryFlagsNetworkQueryIdIndex" on "queryFlags" ("networkId");