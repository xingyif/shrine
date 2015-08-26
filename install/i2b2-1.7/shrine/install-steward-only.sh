#!/bin/bash

#########
# SHRINE Data Steward Installation - Standlone
#
# A script intended for users with an existing SHRINE installation (1.18.x or 
# lower) who simply want to get the SHRINE Data Steward component installed.
#
# - Creates the stewardDB database with the appropriate schema
# - Downloads steward.war and places it in webapps/
# - Templates out steward.conf and steward.xml
#
# @see upgrade_tomcat.sh        = ensure Tomcat 7 is installed first!
# @see skel/steward.conf        = steward application config
# @see skel/steward.xml         = steward Tomcat context definition
#
#########

echo "[shrine/install-steward-only.sh] Begin."

source ./shrine.rc

require "${SHRINE_STEWARD_MYSQL_USER}" "SHRINE_STEWARD_MYSQL_USER must be set"
require "${SHRINE_STEWARD_MYSQL_PASSWORD}" "SHRINE_STEWARD_MYSQL_PASSWORD must be set"
require "${SHRINE_STEWARD_MYSQL_HOST}" "SHRINE_STEWARD_MYSQL_HOST must be set"
require "${SHRINE_STEWARD_DB_NAME}" "SHRINE_STEWARD_DB_NAME must be set"
require "${KEYSTORE_FILE}" "KEYSTORE_FILE must be set"
require "${KEYSTORE_PASSWORD}" "KEYSTORE_PASSWORD must be set"

#####
echo "[shrine/install-steward-only.sh] Creating MySQL database $SHRINE_STEWARD_DB_NAME"

mkdir -p work; cd work

interpolate_file ../steward-only.sql "SHRINE_STEWARD_DB_NAME" "$SHRINE_STEWARD_DB_NAME" | \
interpolate "SHRINE_STEWARD_MYSQL_USER" "$SHRINE_STEWARD_MYSQL_USER" | \
interpolate "SHRINE_STEWARD_MYSQL_PASSWORD" "$SHRINE_STEWARD_MYSQL_PASSWORD" | \
interpolate "SHRINE_STEWARD_MYSQL_HOST" "$SHRINE_STEWARD_MYSQL_HOST" > steward-only.sql.interpolated

mysql -u root < steward-only.sql.interpolated

echo "[shrine/install-steward-only.sh] Creating tables for $SHRINE_STEWARD_DB_NAME"

wget ${SHRINE_SVN_URL_BASE}/code/steward/src/main/sql/mysql.ddl
mysql -u $SHRINE_STEWARD_MYSQL_USER -p$SHRINE_STEWARD_MYSQL_PASSWORD -D $SHRINE_STEWARD_DB_NAME < mysql.ddl


#####
echo "[shrine/install-steward-only.sh] Downloading steward.war"

NEXUS_URL_BASE=http://repo.open.med.harvard.edu/nexus/content/groups/public/net/shrine/

SHRINE_STEWARD_ARTIFACT_ID=steward
SHRINE_STEWARD_WAR_FILE=${SHRINE_STEWARD_ARTIFACT_ID}-${SHRINE_VERSION}.war
SHRINE_STEWARD_WAR_FILE_FINAL_NAME=steward.war
SHRINE_STEWARD_WAR_URL=${NEXUS_URL_BASE}/${SHRINE_STEWARD_ARTIFACT_ID}/${SHRINE_VERSION}/${SHRINE_STEWARD_WAR_FILE}

wget --no-clobber  ${SHRINE_STEWARD_WAR_URL}

cp ${SHRINE_STEWARD_WAR_FILE} $SHRINE_TOMCAT_HOME/webapps/${SHRINE_STEWARD_WAR_FILE_FINAL_NAME}

cd ..

#####
echo "[shrine/install-steward-only.sh] Configuring steward.conf"

interpolate_file ./skel/steward.conf "SHRINE_ADAPTER_I2B2_DOMAIN" "$SHRINE_ADAPTER_I2B2_DOMAIN"  | \
interpolate "SHRINE_STEWARD_DB_NAME" "$SHRINE_STEWARD_DB_NAME" | \
interpolate "I2B2_PM_IP" "$I2B2_PM_IP" | \
interpolate "KEYSTORE_FILE" "$KEYSTORE_FILE" | \
interpolate "KEYSTORE_PASSWORD" "$KEYSTORE_PASSWORD" | \
interpolate "KEYSTORE_ALIAS" "$KEYSTORE_ALIAS" > $SHRINE_STEWARD_CONF_FILE

#####
echo "[shrine/install-steward-only.sh] Configuring steward.xml"
interpolate_file ./skel/steward.xml "SHRINE_STEWARD_MYSQL_USER" "$SHRINE_STEWARD_MYSQL_USER" | \
interpolate "SHRINE_STEWARD_MYSQL_PASSWORD" "$SHRINE_STEWARD_MYSQL_PASSWORD" | \
interpolate "SHRINE_STEWARD_MYSQL_HOST" "$SHRINE_STEWARD_MYSQL_HOST" | \
interpolate "SHRINE_STEWARD_DB_NAME" "$SHRINE_STEWARD_DB_NAME" > $SHRINE_TOMCAT_STEWARD_CONF