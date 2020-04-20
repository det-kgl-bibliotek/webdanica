#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

source $SCRIPT_DIR/setenv.sh

WORKFLOW_HOME=$1


if [ -z "$WORKFLOW_HOME" ]; then
   echo "The 'workflow_home' argument is missing (arg #1). Exiting program"
   exit 1
fi

if [ ! -d "$WORKFLOW_HOME" ]; then
   echo "The workflow_home \'$WORKFLOW_HOME\' does not exist. Exiting program"
   exit 1
fi


# look for existence of WEBDANICA_HOME/lib/webdanica-core-$WEBDANICA_VERSION.jar and WEBDANICA_HOME/lib/webdanica-webapp-$WEBDANICA_VERSION.jar
JARFILE1=$WORKFLOW_HOME/lib/webdanica-core-${WEBDANICA_VERSION}.jar


if [ ! -f "$JARFILE1" ]; then
   echo "The required jarfile '$JARFILE1' does not exist. Exiting program"
   exit 1
fi


OPTS2=-Dwebdanica.settings.file=$WORKFLOW_HOME/conf/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$WORKFLOW_HOME/conf/silent_logback.xml 

java $OPTS2 $OPTS3 -cp "$SCRIPT_DIR/lib/*" dk.kb.webdanica.core.tools.FindHarvestLogs $1 $2
