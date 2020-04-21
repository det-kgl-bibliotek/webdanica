#!/usr/bin/env bash

##bash ingestTool.sh $HARVESTLOG_FILE $CRITERIARESULTS_DIR $WORKFLOW_HOME $WEBDANICA_VERSION $NAS_VERSION $PHOENIX_CLIENT_JAR
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

FIRST="$1"
SECOND="$2"



$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" \
	dk.kb.webdanica.core.tools.CriteriaIngestTool \
	"$FIRST" \
	"$SECOND"

