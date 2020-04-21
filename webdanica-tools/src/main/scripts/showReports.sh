#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

#This script is able to show the reports for a specific netarchivesuite JobID found in the metadata file for the job.

#It requires one argument, and has one optional argument --dont-print

#./showReports.sh JobID [--dont-print]

#If the "--dont-print" argument is used, then the reports are not printed to screen,
# and it just reports the names/urls of the reports found.

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH"  dk.kb.webdanica.core.tools.HarvestShowReports $1 $2

