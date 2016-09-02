create table `queriesReceived` (`shrineNodeId` TEXT NOT NULL,`userName` TEXT NOT NULL,`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`topicId` TEXT,`topicName` TEXT,`timeQuerySent` BIGINT NOT NULL,`timeReceived` BIGINT NOT NULL);
create table `executionsStarted` (`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeExecutionStarted` BIGINT NOT NULL);
create table `executionsCompleted` (`networkQueryId` BIGINT NOT NULL,`replyId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeExecutionCompleted` BIGINT NOT NULL);
create table `resultsSent` (`networkQueryId` BIGINT NOT NULL,`replyId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`timeResultsSent` BIGINT NOT NULL);
create table `problems` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`codec` TEXT NOT NULL,`stampText` TEXT NOT NULL,`summary` TEXT NOT NULL,`description` TEXT NOT NULL,`detailsXml` TEXT NOT NULL,`epoch` BIGINT NOT NULL);
create index `idx_epoch` on `problems` (`epoch`)