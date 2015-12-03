#!/bin/bash

echo "[shrine/mysql.sh] Begin."

#########
# SHRINE MySQL setup for Query History and Data Steward
#
#########
source ./shrine.rc

mkdir -p $SHRINE_HOME
mkdir -p work; cd work
#########

echo "[shrine/mysql.sh] creating MySQL DBs $SHRINE_DB_NAME and $SHRINE_STEWARD_DB_NAME";

interpolate_file ../mysql.sql "SHRINE_DB_NAME" "$SHRINE_DB_NAME" | \
interpolate "SHRINE_MYSQL_USER" "$SHRINE_MYSQL_USER" | \
interpolate "SHRINE_MYSQL_PASSWORD" "$SHRINE_MYSQL_PASSWORD" | \
interpolate "SHRINE_MYSQL_HOST" "$SHRINE_MYSQL_HOST" | \
interpolate "SHRINE_STEWARD_DB_NAME" "$SHRINE_STEWARD_DB_NAME" | \
interpolate "SHRINE_STEWARD_MYSQL_USER" "$SHRINE_STEWARD_MYSQL_USER" | \
interpolate "SHRINE_STEWARD_MYSQL_PASSWORD" "$SHRINE_STEWARD_MYSQL_PASSWORD" | \
interpolate "SHRINE_STEWARD_MYSQL_HOST" "$SHRINE_STEWARD_MYSQL_HOST" > mysql.sql.interpolated

mysql -u root < mysql.sql.interpolated

wget ${SHRINE_SVN_URL_BASE}/code/adapter/src/main/resources/adapter.sql

mysql -u $SHRINE_MYSQL_USER -p$SHRINE_MYSQL_PASSWORD -D $SHRINE_DB_NAME < adapter.sql

wget ${SHRINE_SVN_URL_BASE}/code/broadcaster-aggregator/src/main/resources/hub.sql

mysql -u $SHRINE_MYSQL_USER -p$SHRINE_MYSQL_PASSWORD -D $SHRINE_DB_NAME < hub.sql

wget ${SHRINE_SVN_URL_BASE}/code/service/src/main/resources/create_broadcaster_audit_table.sql

mysql -u $SHRINE_MYSQL_USER -p$SHRINE_MYSQL_PASSWORD -D $SHRINE_DB_NAME < create_broadcaster_audit_table.sql

wget ${SHRINE_SVN_URL_BASE}/code/steward/src/main/sql/mysql.ddl

mysql -u $SHRINE_STEWARD_MYSQL_USER -p$SHRINE_STEWARD_MYSQL_PASSWORD -D $SHRINE_STEWARD_DB_NAME < mysql.ddl


echo "[shrine/mysql.sh] Done."

cd ..
