#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh

#Loads a domain-list into webdanica, inserting them into the domains table.

#If the option --accepted is used, the domains are assumed to be fully danica domains,
# and no further processing is to occur on these domains.

#If the option --rejected is used, the domains are assumed to be not danica, and no further processing is to occur on
# these domains.

#Else the domains are ingested with danicastate UNDECIDED.

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH"  dk.kb.webdanica.core.tools.LoadDomains $1 $2
