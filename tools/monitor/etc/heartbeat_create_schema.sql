create table NODE (
    ID integer not null auto_increment primary key,
    NAME varchar(20),
    FAILURE_COUNT integer default 0,
    constraint NODE_NAME_IX unique (NAME)
);

create table HEARTBEAT_FAILURE (
    ID integer not null auto_increment primary key,
    FAILURE_TIME timestamp default CURRENT_TIMESTAMP,
    MESSAGE varchar(512)
);

create table NODE_FAILURE (
    ID integer not null auto_increment primary key,
    FAILURE_ID integer,
    NODE_ID integer,
    constraint FK_NODE_FAILURE_HEARTBEAT_FAILURE_FAILURE_ID foreign key (FAILURE_ID) references HEARTBEAT_FAILURE (ID),
    constraint FK_NODE_FAILURE_NODE_NODE_ID foreign key (NODE_ID) references NODE (ID)
);

create table EMAIL_NOTIFICATION (
    LAST_NOTIFICATION timestamp default CURRENT_TIMESTAMP
);

insert into EMAIL_NOTIFICATION values (DATE_SUB( CURRENT_DATE, INTERVAL 1 DAY ));