#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh



cd "$WORKFLOW_HOME"

HARVESTLOG_FILE="$1"

if [ -z $HARVESTLOG_FILE ]; then
   echo "ERROR: The HARVESTLOG_FILE argument (arg #1) is not set. Exiting program $PROG"
   exit 1
fi

echo "Processing harvestlog: $HARVESTLOG_FILE"
J="$HARVESTLOG_FILE"

## move $J to WORKDIR
NAME=$(basename $J)
HARVESTLOG="$WORKDIR/$NAME"

mv "$J" "$HARVESTLOG"
RESCODE=$?
if [ -z $RESCODE ]; then
   echo "ERROR: Failed to move the file $J to $HARVESTLOG. Exiting program"
   exit 1
fi

## start_progress
bash ${WORKFLOW_HOME}/automatic.sh $HARVESTLOG
RESCODE=$?
if [ -z $RESCODE ]; then
   echo "ERROR: The script '${WORKFLOW_HOME}/automatic.sh' returned $RESCODE. Exiting program"
fi

## move $HARVESTLOG to OLDJOBSDIR
mv "$HARVESTLOG" "$OLDJOBSDIR"
echo
echo "Processing done of harvestlog: $J "

