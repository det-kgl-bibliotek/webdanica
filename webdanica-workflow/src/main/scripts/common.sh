SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))
source $SCRIPT_DIR/setenv.sh

PROG=$(readlink -f ${BASH_SOURCE[1]})

OPTS3=-Dlogback.configurationFile=${LOG_CONFIG}


if [ ! -f "$WEBDANICA_SETTINGSFILE" ]; then
   echo "ERROR: The webdanica settingsfile '$WEBDANICA_SETTINGSFILE' does not exist. Exiting program $PROG"
   exit 1
fi
OPTS2=-Dwebdanica.settings.file=${WEBDANICA_SETTINGSFILE}

if [ ! -f "$NAS_SETTINGSFILE" ]; then
   echo "WARNING: The netarchivesuite settingsfile '$NAS_SETTINGSFILE' does not exist. You may want to correct this in script $PROG"
fi
OPTS1=-Ddk.netarkivet.settings.file=${NAS_SETTINGSFILE}


if [ ! -d "$JAVA_HOME" ]; then
	echo "ERROR: The JAVA_HOME '$JAVA_HOME' does not exist. Exiting program $PROG"
	exit 1
fi

if [ ! -f "$WEBDANICA_WORKFLOW_JAR" ]; then
   echo "ERROR: The WEBDANICA_JAR '$WEBDANICA_WORKFLOW_JAR' does not exist. Maybe the lib folder is missing, or the webdanica VERSION is wrong. Exiting program $PROG"
   exit 1
fi
if [ ! -f "$PHOENIX_CLIENT_JAR" ]; then
   echo "ERROR: The PHOENIX_CLIENT_JAR '$PHOENIX_CLIENT_JAR' does not exist. Maybe the lib folder is missing, or the webdanica VERSION is wrong. Exiting program $PROG"
   exit 1
fi
if [ ! -d ${WEBDANICA_LIBDIR} ]; then
   echo "ERROR: The WEBDANICA_LIBDIR '$WEBDANICA_LIBDIR' does not exist. Exiting program $PROG"
   exit 1
fi
CLASSPATH="$WEBDANICA_WORKFLOW_JAR:$PHOENIX_CLIENT_JAR:${WEBDANICA_LIBDIR}/*"



# check WORKFLOW_HOME
if [ -z ${WORKFLOW_HOME} ]; then
   echo "ERROR: The WORKFLOW_HOME argument (arg #2) is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d ${WORKFLOW_HOME} ]; then
   echo "ERROR: The WORKFLOW_HOME '$WORKFLOW_HOME' does not exist. Exiting program $PROG"
   exit 1
fi






# Check WEBDATADIR
if [ -z ${WEBDATADIR} ]; then
   echo "ERROR: The WEBDATADIR argument is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d ${WEBDATADIR} ]; then
   echo "ERROR: The WEBDATADIR '$WEBDATADIR' does not exist. Exiting program $PROG"
   exit 1
fi


#Check HADOOP_HOME
if [ -z ${HADOOP_HOME} ]; then
   echo "ERROR: The HADOOP_HOME argument is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d ${HADOOP_HOME}  ]; then
   echo "ERROR: The HADOOP_HOME '$HADOOP_HOME' does not exist. Exiting program $PROG"
   exit 1
fi


#Check PIG_HOME
if [ -z ${PIG_HOME} ]; then
   echo "ERROR: The PIG_HOME argument is not set. Exiting program $PROG"
   exit 1
fi

if [ ! -d ${PIG_HOME} ]; then
   echo "ERROR: The PIG_HOME '$PIG_HOME' does not exist. Exiting program $PROG"
   exit 1
fi