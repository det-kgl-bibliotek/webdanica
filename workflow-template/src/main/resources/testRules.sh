#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

PROG=$(readlink -f $BASH_SOURCE[0])

# look for existence of WORKFLOW_HOME/lib/webdanica-core-$WEBDANICA_VERSION.jar
WORKFLOW_HOME=/home/test/automatic-workflow
CONFDIR=$WORKFLOW_HOME/conf
JARFILE=$WORKFLOW_HOME/lib/webdanica-core-0.4.0-SNAPSHOT.jar
if [ ! -f "$JARFILE" ]; then
   echo "The required jarfile '$JARFILE' does not exist. Exiting program $PROG"
   exit 1
fi
OPTS2=-Dwebdanica.settings.file=$CONFDIR/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$CONFDIR/silent_logback.xml 

java $OPTS2 $OPTS3 -cp "$JARFILE:$SCRIPT_DIR/lib/*" dk.kb.webdanica.core.tools.TestRules $1
