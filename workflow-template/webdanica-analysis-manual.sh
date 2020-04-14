#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

SETENV=$SCRIPT_DIR/setenv.sh

PROG=$(readlink -f $BASH_SOURCE[0])
if [ -r "$SETENV" ]; then
  source "$SETENV"
else 
  echo Error: Path to setenv.sh is not correctly set in script $PROG
  exit 1
fi
## Verify JAVA_HOME

if [ ! -d "$JAVA_HOME" ]; then
 echo "ERROR: The JAVA_HOME '$JAVA_HOME' does not exist. Exiting program $PROG"
   exit 1
fi

if [ ! -f $PHOENIX_CLIENT_JAR ]; then
   echo "ERROR: The jarfile '$PHOENIX_CLIENT_JAR' does not exist. Exiting program $PROG"
   exit 1
fi

## Verify existence of conf/.pigbootup verify script
if [ ! -f $PIGBOOTUP_VERIFIER_SCRIPT ]; then
   echo "ERROR: The script '$PIGBOOTUP_VERIFIER_SCRIPT' does not exist. Exiting program $PROG"
   exit 1
fi
## Verify validity of conf/.pigbootup 
RES=$(bash $PIGBOOTUP_VERIFIER_SCRIPT $WORKFLOW_HOME)
if [ "$RES" != "" ]; then
     echo "Pig bootup file '$PIGBOOTUP_FILE' is invalid: '$RES'"    
     exit 1
fi

if [ ! -f $AUTOMATIC_SCRIPT ]; then
   echo "ERROR: The script '$AUTOMATIC_SCRIPT' does not exist. Exiting program"
   exit 1
 fi

cd $WORKFLOW_HOME

HARVESTLOG_FILE=$1

if [ -z $HARVESTLOG_FILE ]; then
   echo "ERROR: The HARVESTLOG_FILE argument (arg #1) is not set. Exiting program $PROG"
   exit 1
fi

echo Processing harvestlog: $HARVESTLOG_FILE
J=$HARVESTLOG_FILE

## move $J to WORKDIR
NAME=$(basename $J)
HARVESTLOG=$WORKDIR/$NAME

mv $J $HARVESTLOG
RESCODE=$?
if [ -z $RESCODE ]; then
   echo "ERROR: Failed to move the file $J to $HARVESTLOG. Exiting program"
   rm $BUSYFILE
   exit 1
fi

## start_progress
bash $AUTOMATIC_SCRIPT $HARVESTLOG $WORKFLOW_HOME $WEBDATADIR $WEBDANICA_VERSION $HADOOP_HOME $PIG_HOME $NAS_VERSION $PHOENIX_CLIENT_JAR
RESCODE=$?
if [ -z $RESCODE ]; then
   echo "ERROR: The $AUTOMATIC_SCRIPT returned $RESCODE. Exiting program"
fi

## move $HARVESTLOG to OLDJOBSDIR
mv $HARVESTLOG $OLDJOBSDIR
echo
echo "Processing done of harvestlog: $J "

