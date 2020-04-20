#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

#This can be used to add seeds we already known to be Danica. This is actually just the loadSeeds.sh
# with the --accepted argument preset.


SEEDSFILE=$1
if [ -z $SEEDSFILE ]; then
 echo ERROR: No seedsfile given as argument. Exiting program $PROG
 exit
fi

if [ ! -f "$SEEDSFILE" ]; then
 echo ERROR: The given seedsfile \"$SEEDSFILE\" does not exist! Exiting program $PROG
 exit
fi

echo Executing $PROG using webdanica settingsfile \"$WEBDANICA_SETTINGSFILE\"

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH dk.kb.webdanica.core.tools.LoadSeeds $SEEDSFILE --accepted
