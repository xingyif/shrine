create table `queriesSent` (`shrineNodeId` TEXT NOT NULL,`userName` TEXT NOT NULL,`networkQueryId` BIGINT NOT NULL,`queryName` TEXT NOT NULL,`queryTopicId` TEXT,`queryTopicName` TEXT,`timeQuerySent` BIGINT NOT NULL);
create table `previousQueries` (`networkId` BIGINT NOT NULL,`userName` TEXT NOT NULL,`domain` TEXT NOT NULL,`queryName` TEXT NOT NULL,`expression` TEXT NOT NULL,`dateCreated` BIGINT NOT NULL,`queryXml` TEXT NOT NULL);
create table `queryFlags` (`networkId` BIGINT NOT NULL,`flagged` BOOLEAN NOT NULL,`flagMessage` TEXT NOT NULL,`changeDate` BIGINT NOT NULL);
