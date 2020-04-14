#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

HARVESTLOG_FILE=$1
WORKFLOW_HOME=$2
WEBDATADIR=$3
WEBDANICA_VERSION=$4
HADOOP_HOME=$5
export PIG_HOME=$6
NAS_VERSION=$7
PHOENIX_CLIENT_JAR=$8

PROG=$(readlink -f $BASH_SOURCE[0])

SEQ_BASEDIR=$WORKFLOW_HOME/SEQ_AUTOMATIC
CRITERIA_RESULTS_BASEDIR=$WORKFLOW_HOME/criteria-results-automatic
CRITERIA_WORKFLOW_SCRIPT=criteria-workflow-alt.sh
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

# check WORKFLOW_HOME
if [ -z $WORKFLOW_HOME ]; then
   echo "ERROR: The WORKFLOW_HOME argument (arg #2) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d $WORKFLOW_HOME ]; then
   echo "ERROR: The WORKFLOW_HOME '$WORKFLOW_HOME' does not exist. Exiting program $PROG"
   exit 1
fi

# Check WEBDATADIR
if [ -z $WEBDATADIR ]; then
   echo "ERROR: The WEBDATADIR argument (arg #3) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d $WEBDATADIR ]; then
   echo "ERROR: The WEBDATADIR '$WEBDATADIR' does not exist. Exiting program $PROG"
   exit 1
fi

# Check WEBDANICA_VERSION
if [ -z $WEBDANICA_VERSION ]; then
   echo "ERROR: The WEBDANICA_VERSION argument (arg #4) is not set. Exiting program $PROG"
   exit 1
fi

#Check HADOOP_HOME
if [ -z $HADOOP_HOME ]; then
   echo "ERROR: The HADOOP_HOME argument (arg #5) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d $HADOOP_HOME  ]; then
   echo "ERROR: The HADOOP_HOME '$HADOOP_HOME' does not exist. Exiting program $PROG"
   exit 1
fi


#Check PIG_HOME
if [ -z $PIG_HOME ]; then
   echo "ERROR: The PIG_HOME argument (arg #6) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d $PIG_HOME ]; then
   echo "ERROR: The PIG_HOME '$PIG_HOME' does not exist. Exiting program $PROG"
   exit 1
fi

#check NAS_VERSION
if [ -z $NAS_VERSION= ]; then
   echo "ERROR: The NAS_VERSION= argument (arg #7) is not set. Exiting program $PROG"
   exit 1
fi


################### check args finished ##########################################

#1) lav parsed-text af det høstede

#Generer et unikt SEQ_DIR i SEQ_BASEDIR (/home/test/SEQ)
TIMESTAMP=$(/bin/date '+%d-%m-%Y-%s')
SEQ_DIR=$SEQ_BASEDIR/$TIMESTAMP
mkdir -p $SEQ_DIR
echo
echo "Starting parsed-workflow on file $HARVESTLOG_FILE .."
bash parsed-workflow.sh $HARVESTLOG_FILE $WEBDATADIR $SEQ_DIR $WORKFLOW_HOME $HADOOP_HOME $WEBDANICA_VERSION $NAS_VERSION
rc=$?
if [[ $rc != 0 ]]; then 
	echo "ERROR: parsed-workflow failed"
        exit $rc
fi
echo "Finished parsed-workflow on file $HARVESTLOG_FILE with success"
echo
#3) lav kriterie-analyse med pig

#Generer et unikt criteria_results_DIR i CRITERIA_RESULTS_BASEDIR (e.g. /home/test/criteria-results/)
CRITERIARESULTS_DIR=$CRITERIA_RESULTS_BASEDIR/$TIMESTAMP
mkdir -p $CRITERIARESULTS_DIR

echo "Executing : bash $CRITERIA_WORKFLOW_SCRIPT $SEQ_DIR $CRITERIARESULTS_DIR $WORKFLOW_HOME $PIG_HOME"
bash $CRITERIA_WORKFLOW_SCRIPT $SEQ_DIR $CRITERIARESULTS_DIR $WORKFLOW_HOME $PIG_HOME
rc=$?
echo
if [[ $rc != 0 ]]; then echo "ERROR: criteria-workflow failed"; exit $rc; fi


#4) Efterprocessering af kriteria-analysen og ingest i databasen
echo
echo "Executing  bash ingestTool.sh $HARVESTLOG_FILE $CRITERIA_RESULTS_DIR $WORKFLOW_HOME $WEBDANICA_VERSION $PHOENIX_CLIENT_JAR"
bash ingestTool.sh $HARVESTLOG_FILE $CRITERIARESULTS_DIR $WORKFLOW_HOME $WEBDANICA_VERSION $NAS_VERSION $PHOENIX_CLIENT_JAR
rc=$?
if [[ $rc != 0 ]]; then echo "ERROR: criteria ingest failed"; exit $rc; fi
echo
echo "Ingest of $HARVESTLOG_FILE was successful"
echo

	
