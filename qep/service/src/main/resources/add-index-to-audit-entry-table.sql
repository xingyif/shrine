ALTER TABLE AUDIT_ENTRY ADD INDEX `IDX_AUDIT_ENTRY_DOMAIN_USERNAME_QUERY_TOPIC`(DOMAIN_NAME, USERNAME, QUERY_TOPIC);
