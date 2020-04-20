#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

# Note that the script might need to be changed according to the database used by the NAS-system (derby or postgresql).

#The seeds are added to a seedslist named 'webdanicaseeds' list. The seedlist is then added to the default
# configuration of the seed's domain if not already present.

#If the domain does not exist in the NAS system, the domain is created, and the seeds added to the webdanica-seeds
# list as before, but all the seeds in the default seedlist created by NAS are disabled by prefixing each seed with a "#'

#The argument are either one seed or a file with seeds.

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.ImportIntoNetarchiveSuite $1
