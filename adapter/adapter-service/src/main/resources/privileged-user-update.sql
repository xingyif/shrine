alter table PRIVILEGED_USER add DOMAIN varchar(254);
alter table PRIVILEGED_USER add OVERRIDE_DATE TIMESTAMP NULL;
alter table PRIVILEGED_USER drop index IX_PRIVILEGED_USER_USERNAME;
alter table PRIVILEGED_USER add unique index IX_PRIVILEGED_USER_USERNAME_DOMAIN (USERNAME, DOMAIN);

