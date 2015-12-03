#!/bin/bash

#########
# SHRINE Tomcat Upgrade
#
# Upgrade from Tomcat 6 to Tomcat 7, which is required for the new Steward
# functionality.
#
# Backs up old Tomcat directory, moves in old SHRINE webapps and lib/ files, 
# and regenerates server.xml.
#
# @see skel/tomcat7_server.xml  = tomcat server config file
# @see skel/shrine.xml          = webapp configuration
#
#########

source ~/shrine.rc
source ~/shrine-aliases.sh

echo "[shrine/upgrade-tomcat.sh] Begin."
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $SHRINE_HOME

echo "[shrine/upgrade-tomcat.sh] Checking Tomcat version..."
VERSION_STRING=`grep "Apache Tomcat Version [0-9].*" $SHRINE_TOMCAT_HOME/RELEASE-NOTES`
VERSION_REGEX="([0-9])\.(.*)$"
MAJOR_TOMCAT_VER=`[[ $VERSION_STRING =~ $VERSION_REGEX ]] && echo ${BASH_REMATCH[1]}`

if [ $MAJOR_TOMCAT_VER -ge 7 ]
then
  echo "[shrine/upgrade-tomcat.sh] Already on Tomcat 7 or newer, aborting."
  exit
else
  echo "[shrine/upgrade-tomcat.sh] Running Tomcat 6 or older, upgrading to Tomcat 7."
fi


SHRINE_TOMCAT_BACKUP_HOME="${SHRINE_TOMCAT_HOME}${MAJOR_TOMCAT_VER}-bak"
echo "[shrine/upgrade-tomcat.sh] Backing up old $SHRINE_TOMCAT_HOME to $SHRINE_TOMCAT_BACKUP_HOME..."
mv -Tf $SHRINE_TOMCAT_HOME $SHRINE_TOMCAT_BACKUP_HOME

echo "[shrine/upgrade-tomcat.sh] Downloading Tomcat 7..."
cd $SCRIPT_DIR
rm -rf work
mkdir -p work
cd work

TOMCAT_VERSION="7.0.59"

TOMCAT_DIR="apache-tomcat-${TOMCAT_VERSION}"

TOMCAT_ZIP_FILE="${TOMCAT_DIR}.zip"

TOMCAT_ZIP_URL="https://archive.apache.org/dist/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/${TOMCAT_ZIP_FILE}"

wget --no-clobber --no-check-certificate ${TOMCAT_ZIP_URL}

echo "[shine/upgrade-tomcat.sh] Unzipping Tomcat 7 to ${SHRINE_TOMCAT_HOME}"

unzip -q ${TOMCAT_ZIP_FILE}
mkdir -p ${SHRINE_HOME}
mv ${TOMCAT_DIR} ${SHRINE_TOMCAT_HOME}

echo "[shrine/upgrade-tomcat.sh] Rebuilding $SHRINE_TOMCAT_SERVER_CONF from Tomcat 7 defaults..."

cd $SCRIPT_DIR

require "${SHRINE_SSL_PORT}" "SHRINE_SSL_PORT must be set"
require "${SHRINE_PORT}" "SHRINE_PORT must be set"
require "${KEYSTORE_FILE}" "KEYSTORE_FILE must be set"
require "${KEYSTORE_PASSWORD}" "KEYSTORE_PASSWORD must be set"

interpolate_file ./skel/tomcat7_server.xml "KEYSTORE_FILE" "$KEYSTORE_FILE" | \
interpolate "KEYSTORE_PASSWORD" "$KEYSTORE_PASSWORD" | \
interpolate "SHRINE_PORT" "$SHRINE_PORT" | \
interpolate "SHRINE_SSL_PORT" "$SHRINE_SSL_PORT" > $SHRINE_TOMCAT_SERVER_CONF

echo "[shrine/upgrade-tomcat.sh] Restoring old webapps..."
cp -pr $SHRINE_TOMCAT_BACKUP_HOME/webapps/shrine* $SHRINE_TOMCAT_HOME/webapps
cp -pr $SHRINE_TOMCAT_BACKUP_HOME/webapps/steward* $SHRINE_TOMCAT_HOME/webapps

echo "[shrine/upgrade-tomcat.sh] Restoring old lib/ files..."
cp -p $SHRINE_TOMCAT_BACKUP_HOME/lib/shrine.conf $SHRINE_TOMCAT_HOME/lib/shrine.conf
cp -p $SHRINE_TOMCAT_BACKUP_HOME/lib/AdapterMappings.xml $SHRINE_TOMCAT_HOME/lib/AdapterMappings.xml
cp -p $SHRINE_TOMCAT_BACKUP_HOME/lib/AdapterMappings.csv $SHRINE_TOMCAT_HOME/lib/AdapterMappings.csv

echo "[shrine/upgrade-tomcat.sh] Restoring old shrine.xml..."
cp -pr $SHRINE_TOMCAT_BACKUP_HOME/conf/Catalina/ $SHRINE_TOMCAT_HOME/conf/Catalina/

echo "[shrine/upgrade-tomcat.sh] Making new bin/ scripts executable..."
chmod +x $SHRINE_TOMCAT_HOME/bin/*.sh

echo "[shrine/upgrade-tomcat.sh] Changing owner of new Tomcat dir to shrine:shrine..."
chown -R shrine:shrine $SHRINE_TOMCAT_HOME

echo "[shrine/upgrade-tomcat.sh] Upgrade complete!"
echo "[shrine/upgrade-tomcat.sh] It is recommended you run shrine/install-steward-only.sh next."