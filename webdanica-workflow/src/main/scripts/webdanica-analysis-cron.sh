#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh


if [ -f "$BUSYFILE" ]; then
   STAT=$(stat -c %y "$BUSYFILE")
   echo WARNING: Analysis-workflow already in progress. The current workflow started at: $STAT  
   echo "If this is not true, delete the file '$BUSYFILE'"
   exit 1
else
	## mark the workflow as being in progress
	touch "$BUSYFILE"
fi



cd "$WORKFLOW_HOME"
FILES=$(bash "${WORKFLOW_HOME}/findharvestlogs.sh" "$WORKFLOW_HOME")
RESCODE=$?
if [ -z $RESCODE ]; then
   echo "ERROR: The script '${WORKFLOW_HOME}/findharvestlogs.sh' failed. Exiting program $PROG"
   rm "$BUSYFILE"
   exit 1	
fi
if [[ ! -z "$FILES" ]]; then
	echo "Found harvest-logs: $FILES"
else
	#echo Found no harvest-logs. No processing needed
	rm "$BUSYFILE"
	exit
fi 


TOTALCOUNT=$(wc -w <<< "$FILES")
COUNT=0
echo "Found $TOTALCOUNT harvestlogs to process"
for J in $FILES; do
	let "COUNT=COUNT+1"
	echo "Processing harvestlog #$COUNT: $J"

	## move $J to WORKDIR
	NAME=$(basename "$J")
	HARVESTLOG_FILE="$WORKDIR/$NAME"

	mv "$J" "$HARVESTLOG_FILE"
	RESCODE=$?
	if [ -z $RESCODE ]; then
	   echo "ERROR: Failed to move the file $J to $HARVESTLOG_FILE. Exiting program $PROG"
	   rm "$BUSYFILE"
	   exit 1
	fi

	## start_progress
	bash ${WORKFLOW_HOME}/automatic.sh "$HARVESTLOG_FILE"
	RESCODE=$?
	if [ -z $RESCODE ]; then
	   echo "ERROR: The script '${WORKFLOW_HOME}/automatic.sh' returned $RESCODE. Moving $HARVESTLOG_FILE to $FAILEDJOBS"
	   mv "$HARVESTLOG_FILE" "$FAILEDJOBS/"
	else
	   echo "Job successful - Moving $HARVESTLOG to $OKJOBS"
	   mv "$HARVESTLOG_FILE" "$OKJOBS/"
	fi

	echo
	echo "Processing done of harvestlog #$COUNT: $J "

done

## Remove busy-file
rm "$BUSYFILE"

TIMESTAMP=$(date)
echo "Processing done of all $COUNT harvestlogs at $TIMESTAMP"

