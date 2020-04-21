SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

WEBDATADIR=/opt/webdanica/ARKIV


#JAVA
export JAVA_HOME=/usr/lib/jvm/java-1.8.0
export PATH=$JAVA_HOME/bin:$PATH


#Workflows
export WORKFLOW_HOME=/opt/workflows/automatic-workflow

mkdir -pv ${WORKFLOW_HOME}/criteria-results-automatic
mkdir -pv ${WORKFLOW_HOME}/SEQ_AUTOMATIC
mkdir -pv ${WORKFLOW_HOME}/logs
mkdir -pv ${WORKFLOW_HOME}/reports
mkdir -pv ${WORKFLOW_HOME}/working

export BUSYFILE=${WORKFLOW_HOME}/.busy
export WORKDIR=${WORKFLOW_HOME}/working

export OLDJOBSDIR=${WORKFLOW_HOME}/oldjobs
mkdir -pv ${OLDJOBSDIR}

export FAILEDJOBS=${OLDJOBSDIR}/failures
mkdir -pv ${FAILEDJOBS}

export OKJOBS=${OLDJOBSDIR}/successes
mkdir -pv ${OKJOBS}

export PIGBOOTUP_FILE=$WORKFLOW_HOME/conf/.pigbootup







#Webdanica
export WEBDANICA_VERSION=2.2-SNAPSHOT
export WEBDANICA_CORE_JAR="$SCRIPT_DIR/lib/webdanica-core-$WEBDANICA_VERSION.jar"
export WEBDANICA_TOOLS_JAR="$SCRIPT_DIR/lib/webdanica-tools-$WEBDANICA_VERSION.jar"
export WEBDANICA_WORKFLOW_JAR="$SCRIPT_DIR/lib/webdanica-workflow-$WEBDANICA_VERSION.jar"
export WEBDANICA_SETTINGSFILE=$SCRIPT_DIR/conf/webdanica_settings.xml
export WEBDANICA_LIBDIR="$SCRIPT_DIR/lib/"

LOG_CONFIG=$SCRIPT_DIR/conf/silent_logback.xml

#NAS
export NAS_VERSION=5.4
export NAS_SETTINGSFILE=$SCRIPT_DIR/conf/settings_NAS_Webdanica.xml

#Hadoop
export HADOOP_HOME=/usr/hdp/current/hadoop-client/
export PIG_HOME=/usr/hdp/current/pig-client
export PHOENIX_CLIENT_JAR=/usr/hdp/current/phoenix-client/phoenix-client.jar










