@echo off

set CONFIG_HOME=.

rem Set up the classpath
call %CONFIG_HOME%\setup-classpath.cmd

@set JVM_ARGS=-Xmx256m -Xms128m

@java %JVM_ARGS% -Dlog4j.configuration=log4j.properties -classpath %CONFIG_CP% net.shrine.utilities.batchquerier.components.BatchQuerierModule %* 