#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

#This exports all danica-seeds from webdanica to a file

#During the export, the danica seeds not already exported are marked them as exported=true,
# and the exportedTime is set to the current date.

#When using the option '--list_already_exported' all danica seeds is written to a file, including those seeds
# previously exported
#$1=--list_already_exported

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.ExportFromWebdanica $1

