#!/bin/bash

CONFIG_HOME=`dirname $0`

#Set up the classpath

source $CONFIG_HOME/setup-classpath.sh

java -Dlog4j.configuration=log4j.properties -cp $CONFIG_CP net.shrine.utilities.audit.AuditReport "$@"