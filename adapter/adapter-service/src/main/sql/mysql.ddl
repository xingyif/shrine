create table `resultsSent` (`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeQuerySent` BIGINT NOT NULL);
create table `executionStarts` (`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeQuerySent` BIGINT NOT NULL);
create table `executionCompletes` (`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeQuerySent` BIGINT NOT NULL);
create table `queryReceived` (`shrineNodeId` TEXT NOT NULL,`userName` TEXT NOT NULL,`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeSent` BIGINT NOT NULL,`topicId` TEXT,`timeReceived` BIGINT NOT NULL)