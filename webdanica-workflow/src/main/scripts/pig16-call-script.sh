#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

INPUT=$1
OUTPUT=$2
SCRIPT=$3

## configure for correct pig version and log4j properties file
## points the GEOIP_FILE to /full/path/to/GeoIP.dat       (MAYBE to be used by webdanica project)

## bash scripts/$callscript \'$prefix$J'/*gz'\' $basedir/$dirname /home/hadoop/scripts/criteriaRun-combo-v1.pig &> $basedir/error-$dirname.log

FULL_PATH_TO_PIGBOOTUP=${WORKFLOW_HOME}/conf/.pigbootup

#export PIG_OPTS="-Dpig.load.default.statements=$FULL_PATH_TO_PIGBOOTUP -verbose:class"
export PIG_OPTS="-Dpig.load.default.statements=$FULL_PATH_TO_PIGBOOTUP"
echo "Using PIG_OPTS: $PIG_OPTS"
 
### LOG4J CONFIGURATION
export LOG4J_CONFIG=${WORKFLOW_HOME}/conf/log4j_hadoop-pig.properties

export LOG4J="-Dlog4j.configuration=file:${LOG4J_CONFIG}"

USAGE="usage: pig-call-script.sh INPUT-files OUTPUT-dir PIG-script"

if [[ -z "$INPUT" ]]; then
    echo "ERROR: Missing INPUT destination. $USAGE"
    exit 1
fi

if [[ -z "$OUTPUT" ]]; then
	echo "ERROR: Missing OUTPUT destination. $USAGE"
    exit 1
fi

if [[ -z "$SCRIPT" ]]; then
    echo "ERROR: Missing SCRIPT value. $USAGE"
    exit 1
fi


## Verify existence of conf/.pigbootup verify script
if [ ! -f ${WORKFLOW_HOME}/verify_pig_bootup.sh ]; then
   echo "ERROR: The script '${WORKFLOW_HOME}/verify_pig_bootup.sh' does not exist. Exiting program $PROG"
   exit 1
fi
## Verify validity of conf/.pigbootup
RES=$(bash ${WORKFLOW_HOME}/verify_pig_bootup.sh ${PIGBOOTUP_FILE})
if [ "$RES" != "" ]; then
     echo "Pig bootup file '$PIGBOOTUP_FILE' is invalid: '$RES'"
     exit 1
fi


#GEOIP_FILE=/home/hadoop/disk5_instans_m001/GeoIP.dat
#LINKDATABASE_HOME=/home/hadoop/disk5_instans_m001/kopi-db
#export GEOIP_FILE LINKDATABASE_HOME

## Lav .started fil baseret på output filen

touch ${OUTPUT}.started

echo "Executing pig call: $PIG_HOME/bin/pig -x local -4 '$LOG4J_CONFIG' -f '$SCRIPT' -param 'input=$INPUT' -param 'output=$OUTPUT'"
$PIG_HOME/bin/pig -x local -4 "$LOG4J_CONFIG" -f "$SCRIPT" -param "input=$INPUT" -param "output=$OUTPUT"

rc=$?
if [[ $rc != 0 ]]; then
	echo "ERROR: pig call failed with exitcode $rc"
	touch ${OUTPUT}.finished.failed
    exit $rc
else 
	touch ${OUTPUT}.finished.success
    exit $rc
fi

