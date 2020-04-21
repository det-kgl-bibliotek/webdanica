#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

HARVESTLOG_FILE=$1

SEQ_BASEDIR=$WORKFLOW_HOME/SEQ_AUTOMATIC
CRITERIA_RESULTS_BASEDIR=$WORKFLOW_HOME/criteria-results-automatic
################### check args ##########################################

# check HARVESTLOG_FILE
if [ -z $HARVESTLOG_FILE ]; then
   echo "ERROR: The HARVESTLOG_FILE argument (arg #1) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -f $HARVESTLOG_FILE ]; then
   echo "ERROR: The harvestlog '$HARVESTLOG_FILE' does not exist. Exiting program $PROG"
   exit 1
fi


################### check args finished ##########################################

#1) lav parsed-text af det høstede

#Generer et unikt SEQ_DIR i SEQ_BASEDIR (/home/test/SEQ)
TIMESTAMP=$(/bin/date '+%d-%m-%Y-%s')
SEQ_DIR="$SEQ_BASEDIR/$TIMESTAMP"
mkdir -p "$SEQ_DIR"
echo
echo "Starting parsed-workflow on file $HARVESTLOG_FILE .."
bash parsed-workflow.sh "$HARVESTLOG_FILE" "$SEQ_DIR"
rc=$?
if [[ $rc != 0 ]]; then 
	echo "ERROR: parsed-workflow failed"
        exit $rc
fi
echo "Finished parsed-workflow on file $HARVESTLOG_FILE with success"
echo

#3) lav kriterie-analyse med pig

#Generer et unikt criteria_results_DIR i CRITERIA_RESULTS_BASEDIR (e.g. /home/test/criteria-results/)
CRITERIARESULTS_DIR="$CRITERIA_RESULTS_BASEDIR/$TIMESTAMP"
mkdir -p "$CRITERIARESULTS_DIR"

CRITERIA_WORKFLOW_SCRIPT=criteria-workflow-alt.sh
echo "Executing : bash $CRITERIA_WORKFLOW_SCRIPT $SEQ_DIR $CRITERIARESULTS_DIR "
bash "$CRITERIA_WORKFLOW_SCRIPT" "$SEQ_DIR" "$CRITERIARESULTS_DIR"
rc=$?
echo
if [[ $rc != 0 ]]; then echo "ERROR: criteria-workflow failed"; exit $rc; fi


#4) Efterprocessering af kriteria-analysen og ingest i databasen
echo
echo "Executing  bash ingestTool.sh $HARVESTLOG_FILE $CRITERIARESULTS_DIR"
bash ingestTool.sh "$HARVESTLOG_FILE" "$CRITERIARESULTS_DIR"
rc=$?
if [[ $rc != 0 ]]; then echo "ERROR: criteria ingest failed"; exit $rc; fi
echo
echo "Ingest of $HARVESTLOG_FILE was successful"
echo

	
