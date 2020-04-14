#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

# replace path in WORKFLOW_HOME with the correct full path
#WORKFLOW_HOME=/REPLACE/WITH/CORRECT/FULL/PATH
WORKFLOW_HOME=${WORKFLOW_HOME:-/home/test/workflow}

PROG=$(readlink -f $BASH_SOURCE[0])

source "$SCRIPT_DIR/setenv.sh"


if [ ! -d "$WORKFLOW_HOME" ]; then
  echo ERROR: The WORKFLOW_HOME \"$WORKFLOW_HOME\" does not exist. Please correct the path in $PROG
  exit 1
fi

NAS_SETTINGSFILE=$WORKFLOW_HOME/conf/settings_NAS_Webdanica.xml
WEBDANICA_SETTINGSFILE=$WORKFLOW_HOME/conf/webdanica_settings.xml

if [ ! -f "$WEBDANICA_SETTINGSFILE" ]; then
 echo ERROR: Webdanica settingsfile \"$WEBDANICA_SETTINGSFILE\" does not exist. Please correct the path in $PROG
 exit
fi

OPTS1=-Ddk.netarkivet.settings.file=$NAS_SETTINGSFILE
OPTS2=-Dwebdanica.settings.file=$WEBDANICA_SETTINGSFILE
OPTS3=-Dlogback.configurationFile=$WORKFLOW_HOME/conf/silent_logback.xml 


WEBDANICA_JAR="$SCRIPT_DIR/lib/webdanica-core-$VERSION.jar"
if [ ! -f "$WEBDANICA_JAR" ]; then
 echo ERROR: The Webdanica-core.jar file \"$WEBDANICA_JAR\" does not exist. The version might be incorrect, or the lib folder is missing.
 exit
fi

echo Executing $PROG using webdanica settingsfile \"$WEBDANICA_SETTINGSFILE\"

CLASSPATH="$WEBDANICA_JAR:\
$SCRIPT_DIR/lib/slf4j-api-1.7.7.jar:\
$SCRIPT_DIR/lib/commons-io-2.0.1.jar:\
$SCRIPT_DIR/lib/common-core-$NAS_VERSION.jar:\
$SCRIPT_DIR/lib/harvester-core-$NAS_VERSION.jar:\
$SCRIPT_DIR/lib/derbyclient-10.12.1.1.jar:\
$SCRIPT_DIR/lib/jwat-common-1.0.4.jar:\
$SCRIPT_DIR/lib/guava-11.0.2.jar:\
$SCRIPT_DIR/lib/archive-core-$NAS_VERSION.jar:\
$SCRIPT_DIR/lib/postgresql-9.2-1003-jdbc4.jar"

java "$OPTS1" "$OPTS2" "$OPTS3" -cp "$CLASSPATH" dk.kb.webdanica.core.tools.Harvest $1
