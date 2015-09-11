create table `resultsSent` (`networkQueryId` BIGINT NOT NULL,`replyId` BIGINT NOT NULL, `queryName` TEXT NOT NULL,`timeResultsSent` BIGINT NOT NULL);
create table `executionStarts` (`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeExecutionStarts` BIGINT NOT NULL);
create table `executionCompletes` (`networkQueryId` BIGINT NOT NULL,`replyId` BIGINT NOT NULL, `queryName` TEXT NOT NULL,`timeExecutionCompletes` BIGINT NOT NULL);
create table `queryReceived` (`shrineNodeId` TEXT NOT NULL,`userName` TEXT NOT NULL,`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeSent` BIGINT NOT NULL,`topicId` TEXT,`topicName` TEXT,`timeReceived` BIGINT NOT NULL)
