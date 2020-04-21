#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

HARVESTLOG_FILE="$1"
SEQDIR="$2"


if [ -z "$HARVESTLOG_FILE" ]; then
   echo "The 'HARVESTLOG' argument is missing (arg #1). Exiting program $PROG"
   exit 1
 fi


if [ ! -f "$HARVESTLOG_FILE" ]; then
   echo "The harvestlog $HARVESTLOG_FILE does not exist. Exiting program $PROG"
   exit 1
fi


if [ -z "$SEQDIR" ]; then
   echo "The 'SEQDIR' argument is missing (arg #3). Exiting program $PROG"
   exit 1
fi

if [ ! -d "$SEQDIR" ]; then
   echo "The seqdir '$SEQDIR' does not exist. Exiting program $PROG"
   exit 1
fi


echo "Calling 'bash findwarcs.sh $HARVESTLOG_FILE'"

WARCS=$(bash findwarcs.sh $HARVESTLOG_FILE )
RESCODE=$?
if [ $RESCODE -ne 0 ]; then
   echo "ERROR: The script 'findwarcs.sh' failed with statuscode $RESCODE. Exiting program $PROG"
   exit 1       
fi
echo "Found warcs: $WARCS"
failures=0
successes=0
for WARC in $WARCS; do
	echo "Processing $WARC"
	DESTINATION=$SEQDIR/$(basename $WARC)
	mkdir -p "$DESTINATION"
	echo "do parsed-extract on file $WARC with destination $DESTINATION"
	bash parse-text-extraction.sh "$WARC" "$DESTINATION" &>> logs/parsed.log
	RESCODE=$?
	if [ $RESCODE -ne 0 ]; then
	   echo "ERROR: The call to parse-text-extraction.sh on file $WARC failed"
	   failures=$((failures+1))
	else
	   successes=$((successes+1))
	fi
done

if [ "$successes" -gt 0 ]; then
    echo "Considering command successful: #successes=$successes,#failures=$failures"
    exit 0		
else
    exit 1
fi
