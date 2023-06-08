#!/bin/sh

# Run script to be packaged inside docker container for starting the spring-boot service
JVM_OPTIONS=$*

# jarfile is expected in the same directory as the run scripts
DIRNAME=`dirname $0`

echo "[INFO] Run: java $JVM_OPTIONS -jar $DIRNAME/service.jar"
exec java $JVM_OPTIONS -jar $DIRNAME/service.jar