#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

#Takes one argument: a seedsfile, or two arguments: a seedsfile --accepted
#TODO --onlysavestats

#The result of this operation is added to the ingestlog table,
# and a rejectlog and an acceptlog is written to the same directory the seedsfile.

#If the '--accepted' option is used, the seeds are declared with DanicaStatus.YES when they are inserted.

#If the seed is already registered as a danica-seed, nothing happens.

#If the seed is already registered as a not-danica-seed, the danicastate of the seed is changed to danica<br/>

#Otherwise, the seed is registered as a danica-seed, and the domain of the seed created in the domains table<br/>


echo Executing $PROG using webdanica settingsfile \"$WEBDANICA_SETTINGSFILE\"

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH"  dk.kb.webdanica.core.tools.LoadSeeds $1 $2 $3
