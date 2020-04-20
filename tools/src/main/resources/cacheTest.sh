#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

#This tool is used to update the statecache in hbase.
#
#It has no arguments.

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.datamodel.Cache
