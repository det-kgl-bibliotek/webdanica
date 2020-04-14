#!/usr/bin/env bash

WORKFLOW_HOME=$1
WEBDANICA_VERSION=$2
NAS_VERSION=$3

if [ -z "$WORKFLOW_HOME" ]; then
   echo "The 'workflow_home' argument is missing (arg #1). Exiting program"
   exit 1
fi

if [ ! -d "$WORKFLOW_HOME" ]; then
   echo "The workflow_home \'$WORKFLOW_HOME\' does not exist. Exiting program"
   exit 1
fi

if [ -z "$WEBDANICA_VERSION" ]; then
   echo "The 'webdanica_version' argument is missing (arg #2). Exiting program"
   exit 1
 fi

if [ -z "$NAS_VERSION" ]; then
   echo "The 'nas_version' argument is missing (arg #3). Exiting program"
   exit 1
 fi


# look for existence of WEBDANICA_HOME/lib/webdanica-core-$WEBDANICA_VERSION.jar and WEBDANICA_HOME/lib/webdanica-webapp-$WEBDANICA_VERSION.jar
JARFILE1=$WORKFLOW_HOME/lib/webdanica-core-${WEBDANICA_VERSION}.jar
JARFILE2=$WORKFLOW_HOME/lib/webdanica-webapp-${WEBDANICA_VERSION}.jar


if [ ! -f "$JARFILE1" ]; then
   echo "The required jarfile '$JARFILE1' does not exist. Exiting program"
   exit 1
fi

if [ ! -f "$JARFILE2" ]; then
   echo "The required jarfile '$JARFILE2' does not exist. Exiting program"
   exit 1
fi

OPTS2=-Dwebdanica.settings.file=$WORKFLOW_HOME/conf/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$WORKFLOW_HOME/conf/silent_logback.xml 

java $OPTS2 $OPTS3 -cp $JARFILE1:$JARFILE2:lib/slf4j-api-1.7.7.jar:lib/slf4j-log4j12-1.7.12.jar:lib/commons-io-2.0.1.jar:lib/common-core-$NAS_VERSION.jar:lib/harvester-core-$NAS_VERSION.jar:lib/archive-core-$NAS_VERSION.jar dk.kb.webdanica.webapp.tools.FindHarvestLogs $1 $2
