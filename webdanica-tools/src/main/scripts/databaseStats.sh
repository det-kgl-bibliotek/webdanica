#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

#This tool is used to show the statistics of the webdanica tables.
#
#The output looks like this:

#Seeds-stats at 'Wed Sep 27 12:30:46 CEST 2017':
#=========================================
#Total-seeds: 0
##seeds with status 'NEW': 0
##seeds with status 'READY_FOR_HARVESTING': 0
##seeds with status 'HARVESTING_IN_PROGRESS': 0
##seeds with status 'HARVESTING_FINISHED': 0
##seeds with status 'READY_FOR_ANALYSIS': 0
##seeds with status 'ANALYSIS_COMPLETED': 0
##seeds with status 'REJECTED': 0
##seeds with status 'AWAITS_CURATOR_DECISION': 0
##seeds with status 'HARVESTING_FAILED': 0
##seeds with status 'DONE': 0
##seeds with status 'ANALYSIS_FAILURE': 0
#Total number of entries in 'harvests' table: 0
#Total number of entries in 'criteria_results' table: 0
#Time spent computing the stats in secs: 22

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.ComputeStats
