#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

SEQ_BASEDIR=$1
CRITERIARESULTSDIR=$2

USAGE="USAGE: bash criteria-worklow.sh <seq_basedir> <criteriaresultsdir>"


if [ -z "$1" ]; then
 echo ERROR: No SEQ_BASEDIR argument is given!
 echo $USAGE
 exit 1
fi
if [ -z "$2" ]; then
 echo ERROR: No CRITERIARESULTSDIR argument is given!
 echo $USAGE
 exit 1
fi


SCRIPTPATH=$WORKFLOW_HOME/pigscripts/criteriaRun-combinedCombo-alt-seq.pig

SEQDIRS=$(ls -1 $SEQ_BASEDIR)

for J in $SEQDIRS; do
	FILE="$SEQ_BASEDIR/$J/$J"
	if [ ! -f "$FILE" ]; then
		echo "ERROR: seqfile $FILE does not exist. The parsed-text computation must have gone wrong"
		exit 1
	fi

	## TODO look for the SUCCESS file in the $SEQBASEDIR/$J directory
	DESTINATION="$CRITERIARESULTSDIR/$J"
	echo "do criteria-analysis on file $FILE with destination $DESTINATION"
	bash pig16-call-script.sh "$FILE" "$DESTINATION" "$SCRIPTPATH"
	rc=$?
	if [[ $rc != 0 ]]; then
		echo "ERROR: criteria-analysis on file $FILE with destination $DESTINATION: failed with exitcode $rc"
	    exit $rc
	fi
done

