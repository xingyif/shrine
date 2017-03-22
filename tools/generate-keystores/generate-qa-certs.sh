#!/bin/bash

# The host that contains the hub
HUB_HOST=shrine-qa1
# The hosts we want to generate keystores for
QA_HOSTS=($HUB_HOST shrine-qa2 shrine-qa3 telesto)
# The root CA for the https certs
SHRINE_HTTPS_ROOT=shrine-https-root # The root for the https certs
# The root ca for SHRINE's query signing certificates
SHRINE_HUB=shrine-hub
# The password for the keystore. Taken in on the command line
PASS=$1
USE_HTTPS_ROOT=0
HTTPS_FLAG="--https-root"
USAGE="$0 [--https-root] keystore-password"

# Check if password is empty, or if flag is used without a following password
if [ "$PASS" == "" ]
then
  exec echo $USAGE
fi

if [ "$PASS" == $HTTPS_FLAG ] && [ "$2" != "" ]
then
  PASS="$2"
  USE_HTTPS_ROOT=1
elif [ "$PASS" == $HTTPS_FLAG ]
then # Flag without password
  exec echo $USAGE
fi

# Check that password is longer than 6 characters
if [ ${#PASS} -lt 6 ]; then
  exec echo "Password must be 6 characters or longer"
fi

mkdir -p $SHRINE_HUB
mkdir -p $SHRINE_HTTPS_ROOT

# Generate the https root CA
openssl req -x509 -new -newkey rsa:2048 -keyout $SHRINE_HTTPS_ROOT/$SHRINE_HTTPS_ROOT.key -sha256 -nodes -days 1024 -out $SHRINE_HTTPS_ROOT/$SHRINE_HTTPS_ROOT.crt -subj "/C=US/ST=Massachusetts/L=Boston/O=Harvard Medical School/OU=Catalyst SHRINE QA Network/CN=$SHRINE_HTTPS_ROOT"

# Generate the hub CA
openssl req -x509 -new -newkey rsa:2048 -keyout $SHRINE_HUB/$SHRINE_HUB.key -sha256 -nodes -days 1024 -out $SHRINE_HUB/$SHRINE_HUB.crt -subj "/C=US/ST=Massachusetts/L=Boston/O=Harvard Medical School/OU=Catalyst SHRINE QA Network/CN=$SHRINE_HUB"

for HOST in ${QA_HOSTS[*]}; do
  mkdir -p $HOST
  # Generate the query signing certificate CSR
  openssl req -new -newkey rsa:2048 -keyout $HOST/$HOST.catalyst.key -sha256 -nodes -days 1024 -out $HOST/$HOST.catalyst.csr -config openssl.cnf -subj "/C=US/ST=Massachusetts/L=Boston/O=Harvard Medical School/OU=Catalyst SHRINE QA Hub/CN=$HOST.catalyst"
  # Generate the https serving certificate CSR
  openssl req -new -newkey rsa:2048 -keyout $HOST/$HOST-https.catalyst.key -sha256 -nodes -days 1024 -out $HOST/$HOST-https.catalyst.csr -config openssl.cnf -subj "/C=US/ST=Massachusetts/L=Boston/O=Harvard Medical School/OU=Catalyst SHRINE QA Hub/CN=$HOST-https.catalyst"

  # Sign the signing certs with the hub CA and the https certs with the https root CA
  openssl x509 -req -in $HOST/$HOST.catalyst.csr -CA $SHRINE_HUB/$SHRINE_HUB.crt -CAkey $SHRINE_HUB/$SHRINE_HUB.key -CAcreateserial -out $HOST/$HOST.catalyst.crt -days 1024 -sha256
  openssl x509 -req -in $HOST/$HOST-https.catalyst.csr -CA $SHRINE_HTTPS_ROOT/$SHRINE_HTTPS_ROOT.crt -CAkey $SHRINE_HTTPS_ROOT/$SHRINE_HTTPS_ROOT.key -CAcreateserial -out $HOST/$HOST-https.catalyst.crt -days 1024 -sha256

  # Generate pkcs12 keystore
  openssl pkcs12 -export -in $HOST/$HOST.catalyst.crt -inkey $HOST/$HOST.catalyst.key -out $HOST/$HOST.catalyst.p12 -name $HOST.catalyst -CAfile $SHRINE_HUB/$SHRINE_HUB.crt -caname $SHRINE_HUB -chain -password pass:$PASS

  # Convert pcks12 keystores to jks keystores
  keytool -importkeystore -srckeystore $HOST/$HOST.catalyst.p12 -srcstoretype pkcs12 -srcalias $HOST.catalyst -srcstorepass $PASS -destkeystore $HOST/$HOST.catalyst.jks -deststorepass $PASS -deststoretype jks  -destalias $HOST.catalyst

  # Import certificates into keystore
  keytool -importcert -alias $SHRINE_HUB -noprompt -file $SHRINE_HUB/$SHRINE_HUB.crt -keystore $HOST/$HOST.catalyst.jks -storepass $PASS
  keytool -importcert -alias $HOST-https.catalyst -noprompt -file $HOST/$HOST-https.catalyst.crt -keystore $HOST/$HOST.catalyst.jks -storepass $PASS
done

if [ $USE_HTTPS_ROOT -eq 0 ]; then
  # Import the hub's https cert into all downstream nodes, and all downstream node's https certs into the hub's keystore
  for HOST in ${QA_HOSTS[*]:1}; do
    keytool -importcert -alias $SHRINE_HUB-https -noprompt -file $HUB_HOST/$HUB_HOST-https.catalyst.crt -keystore $HOST/$HOST.catalyst.jks -storepass $PASS
    keytool -importcert -alias $HOST-https -noprompt -file $HOST/$HOST-https.catalyst.crt -keystore $HUB_HOST/$HUB_HOST.catalyst.jks -storepass $PASS
  done
else
  # Import the root https cert into the hub and downstream keystores
  for HOST in ${QA_HOSTS[*]}; do
    keytool -importcert -alias $SHRINE_HTTPS_ROOT -noprompt -file $SHRINE_HTTPS_ROOT/$SHRINE_HTTPS_ROOT.crt -keystore $HOST/$HOST.catalyst.jks -storepass $PASS
  done
fi


for HOST in ${QA_HOSTS[*]}; do
  keytool -list -noprompt -keystore $HOST/$HOST.catalyst.jks -storepass $PASS
done


