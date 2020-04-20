SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
#JAVA
JAVA_HOME=/usr/lib/jvm/java-1.8.0
PATH=$JAVA_HOME/bin:$PATH


#Workflows
export WORKFLOW_USER_HOME=/opt/workflows
export WORKFLOW_HOME=$WORKFLOW_USER_HOME/automatic-workflow
export AUTOMATIC_SCRIPT=${WORKFLOW_HOME}/automatic.sh
export FINDLOGS_SCRIPT=${WORKFLOW_HOME}/findharvestlogs.sh
export PIGBOOTUP_VERIFIER_SCRIPT=$WORKFLOW_HOME/verify_pig_bootup.sh

export PIGBOOTUP_FILE=$WORKFLOW_HOME/conf/.pigbootup
export BUSYFILE=$WORKFLOW_HOME/.busy
export WORKDIR=$WORKFLOW_HOME/working
export OLDJOBSDIR=$WORKFLOW_HOME/oldjobs
mkdir -pv $OLDJOBSDIR
export FAILEDJOBS=$OLDJOBSDIR/failures
mkdir -pv $FAILEDJOBS
export OKJOBS=$OLDJOBSDIR/successes
mkdir -pv $OKJOBS

mkdir -pv $WORKFLOW_HOME/criteria-results-automatic
mkdir -pv $WORKFLOW_HOME/SEQ_AUTOMATIC
mkdir -pv $WORKFLOW_HOME/logs
mkdir -pv $WORKFLOW_HOME/reports
mkdir -pv $WORKFLOW_HOME/working





#Webdanica
export WEBDANICA_VERSION=2.2-SNAPSHOT
export WEBDANICA_JAR="$SCRIPT_DIR/lib/webdanica-core-$WEBDANICA_VERSION.jar"
export WEBDANICA_SETTINGSFILE=$SCRIPT_DIR/conf/webdanica_settings.xml
export WEBDANICA_LIBDIR="$SCRIPT_DIR/lib/"

#NAS
export NAS_VERSION=5.4
export NAS_SETTINGSFILE=$SCRIPT_DIR/conf/settings_NAS_Webdanica.xml

#Hadoop
export HADOOP_HOME=/usr/hdp/current/hadoop-client/
export PIG_HOME=/usr/hdp/current/pig-client
export PHOENIX_CLIENT_JAR=/usr/hdp/current/phoenix-client/phoenix-client.jar


LOG_CONFIG=$SCRIPT_DIR/conf/silent_logback.xml
WEBDATADIR=/opt/webdanica/ARKIV








