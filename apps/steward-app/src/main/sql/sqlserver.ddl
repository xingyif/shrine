create table `users` (`userName` VARCHAR(254) NOT NULL PRIMARY KEY,`fullName` VARCHAR(254) NOT NULL,`isSteward` BOOLEAN NOT NULL);
create table `topics` (`id` INTEGER NOT NULL,`name` VARCHAR(254) NOT NULL,`description` VARCHAR(MAX) NOT NULL,`createdBy` VARCHAR(254) NOT NULL,`createDate` BIGINT NOT NULL,`state` VARCHAR(254) NOT NULL,`changedBy` VARCHAR(254) NOT NULL,`changeDate` BIGINT NOT NULL);
create index `changeDateIndex` on `topics` (`changeDate`);
create index `changedByIndex` on `topics` (`changedBy`);
create index `createDateIndex` on `topics` (`createDate`);
create index `createdByIndex` on `topics` (`createdBy`);
create index `idIndex` on `topics` (`id`);
create index `stateIndex` on `topics` (`state`);
create index `topicNameIndex` on `topics` (`name`);
create table `queries` (`stewardId` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`id` BIGINT NOT NULL,`name` VARCHAR(254) NOT NULL,`researcher` VARCHAR(254) NOT NULL,`topic` INTEGER,`queryContents` VARCHAR(MAX) NOT NULL,`stewardResponse` VARCHAR(254) NOT NULL,`date` BIGINT NOT NULL);
create index `dateIndex` on `queries` (`date`);
create index `externalIdIndex` on `queries` (`id`);
create index `queryNameIndex` on `queries` (`name`);
create index `researcherIdIndex` on `queries` (`stewardId`);
create index `stewardResponseIndex` on `queries` (`stewardResponse`);
create index `topicIdIndex` on `queries` (`topic`);
create table `userTopic` (`researcher` VARCHAR(254) NOT NULL,`topicId` INTEGER NOT NULL,`state` VARCHAR(254) NOT NULL,`changedBy` VARCHAR(254) NOT NULL,`changeDate` BIGINT NOT NULL);
create unique index `researcherTopicIdIndex` on `userTopic` (`researcher`,`topicId`);
