#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

HARVESTLOG_FILE=$1


if [ -z "$HARVESTLOG_FILE" ]; then
   echo "The 'harvestlog' argument is missing (arg #1). Exiting program $PROG"
   exit 1
fi

if [ ! -f "$HARVESTLOG_FILE" ]; then
   echo "The harvestlog $HARVESTLOG_FILE does not exist. Exiting program $PROG"
   exit 1
fi

if [ -z "$DATADIR" ]; then
   echo "The 'datadir' argument is missing (arg #2). Exiting program $PROG"
   exit 1
fi





$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.FindHarvestWarcs $HARVESTLOG_FILE $WEBDATADIR
