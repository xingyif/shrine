create table "queriesReceived" ("shrineNodeId" VARCHAR2(256) NOT NULL,"userName" VARCHAR2(256) NOT NULL,"networkQueryId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"topicId" VARCHAR2(256),"topicName" VARCHAR2(256),"timeQuerySent" NUMBER NOT NULL,"timeReceived" NUMBER NOT NULL);
create table "executionsStarted" ("networkQueryId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeExecutionStarted" NUMBER NOT NULL);
create table "executionsCompleted" ("networkQueryId" NUMBER NOT NULL,"replyId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeExecutionCompleted" NUMBER NOT NULL);
create table "resultsSent" ("networkQueryId" NUMBER NOT NULL,"replyId" NUMBER NOT NULL,"queryName" VARCHAR2(256) NOT NULL,"timeResultsSent" NUMBER NOT NULL);
create table "problems" ("id" INTEGER NOT NULL PRIMARY KEY,"codec" VARCHAR2(254) NOT NULL,"stampText" VARCHAR2(500) NOT NULL,"summary" VARCHAR2(500) NOT NULL,"description" VARCHAR2(1000) NOT NULL,"detailsXml" CLOB NOT NULL,"epoch" NUMBER(19) NOT NULL);
create index "idx_epoch" on "problems" ("epoch");
create SEQUENCE problems_idautoinc;
create or REPLACE TRIGGER problems_triggerid_id BEFORE INSERT ON
            "problems" FOR EACH ROW
            BEGIN
              SELECT problems_idautoinc.NEXTVAL
              into :NEW."id"
              FROM   dual;
            END;