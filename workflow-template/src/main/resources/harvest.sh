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


if [ ! -f "$WEBDANICA_JAR" ]; then
 echo ERROR: The Webdanica-core.jar file \"$WEBDANICA_JAR\" does not exist. The version might be incorrect, or the lib folder is missing.
 exit
fi

echo Executing $PROG using webdanica settingsfile \"$WEBDANICA_SETTINGSFILE\"

CLASSPATH="$WEBDANICA_JAR:\
$SCRIPT_DIR/lib/*"

java "$OPTS1" "$OPTS2" "$OPTS3" -cp "$CLASSPATH" dk.kb.webdanica.core.tools.Harvest $1
