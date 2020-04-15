#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

HARVESTLOG=$1
DATADIR=$2
WORKFLOW_HOME=$3
WEBDANICA_VERSION=$4
NAS_VERSION=$5

PROG=$(readlink -f $BASH_SOURCE[0])

if [ -z "$HARVESTLOG" ]; then
   echo "The 'harvestlog' argument is missing (arg #1). Exiting program $PROG"
   exit 1
fi

if [ ! -f "$HARVESTLOG" ]; then
   echo "The harvestlog $HARVESTLOG does not exist. Exiting program $PROG"
   exit 1
fi

if [ -z "$DATADIR" ]; then
   echo "The 'datadir' argument is missing (arg #2). Exiting program $PROG"
   exit 1
fi

if [ ! -d "$DATADIR" ]; then
   echo "The datadir '$DATADIR' does not exist. Exiting program $PROG"
   exit 1
fi

if [ -z "$WORKFLOW_HOME" ]; then
   echo "The 'workflow_home' argument is missing (arg #3). Exiting program $PROG"
   exit 1
fi

if [ ! -d "$WORKFLOW_HOME" ]; then
   echo "The WORKFLOW_HOME argument \'$WORKFLOW_HOME\' does not exist. Exiting program $PROG"
   exit 1
fi

if [ -z "$WEBDANICA_VERSION" ]; then
   echo "The 'webdanica_version' argument is missing (arg #4). Exiting program $PROG"
   exit 1
fi

if [ -z "$NAS_VERSION" ]; then
   echo "The 'nas_version' argument is missing (arg #5). Exiting program $PROG"
   exit 1
fi


# look for existence of WORKFLOW_HOME/lib/webdanica-core-$WEBDANICA_VERSION.jar
JARFILE=$WORKFLOW_HOME/lib/webdanica-core-${WEBDANICA_VERSION}.jar
if [ ! -f "$JARFILE" ]; then
   echo "The required jarfile '$JARFILE' does not exist. Exiting program $PROG"
   exit 1
fi

OPTS2=-Dwebdanica.settings.file=$WORKFLOW_HOME/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$WORKFLOW_HOME/silent_logback.xml 

CLASSPATH="$JARFILE:$SCRIPT_DIR/lib/*"

java $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.FindHarvestWarcs $1 $2
