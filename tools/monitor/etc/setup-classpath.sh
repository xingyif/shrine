#!/bin/bash

export CONFIG_CP=.

#
#Set up our classpath with our library dependencies...
for jar in `find $CONFIG_HOME/lib/*.jar`;
do
    CONFIG_CP=$jar:$CONFIG_CP
done

if [ ${#JAVA_HOME} == 0 ]; then
    JAVA_HOME=/usr/local/java
fi

JVM_ARGS="-Xms256m -Xmx1024m -verbose:gc -Xloggc:gc.log"