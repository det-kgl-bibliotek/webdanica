#!/usr/bin/env bash

##bash ingestTool.sh $HARVESTLOG_FILE $CRITERIARESULTS_DIR $WORKFLOW_HOME $WEBDANICA_VERSION $NAS_VERSION $PHOENIX_CLIENT_JAR
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

FIRST="$1"
SECOND="$2"

WORKFLOW_HOME="$3"
WEBDANICA_VERSION="$4"
NAS_VERSION="$5"
PHOENIX_CLIENT_JAR="$6"

SETTINGSFILE="$WORKFLOW_HOME/conf/webdanica_settings.xml"

if [ ! -f "$SETTINGSFILE" ]; then
   echo "The SETTINGSFILE \'$SETTINGSFILE\' does not exist. Exiting program"
   exit 1
fi

OPTS2="-Dwebdanica.settings.file=$SETTINGSFILE"
OPTS3="-Dlogback.configurationFile=$WORKFLOW_HOME/conf/silent_logback.xml"

CLASSPATH="$SCRIPT_DIR/lib/webdanica-core-$WEBDANICA_VERSION.jar:\
$PHOENIX_CLIENT_JAR:\
$SCRIPT_DIR/lib/*"

java "$OPTS2" \
	"$OPTS3" \
	-cp "$CLASSPATH" \
	dk.kb.webdanica.core.tools.CriteriaIngestTool \
	"$FIRST" \
	"$SECOND"

