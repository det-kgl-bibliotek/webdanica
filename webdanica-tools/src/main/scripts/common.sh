SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

source $SCRIPT_DIR/setenv.sh

OPTS1=-Ddk.netarkivet.settings.file=$NAS_SETTINGSFILE
OPTS2=-Dwebdanica.settings.file=$WEBDANICA_SETTINGSFILE
OPTS3=-Dlogback.configurationFile=$LOG_CONFIG

if [ ! -d "$SCRIPT_DIR" ]; then
  echo "ERROR: The TOOLS_HOME '$SCRIPT_DIR' does not exist. Please correct the path in $PROG"
  exit 1
fi

if [ ! -f "$WEBDANICA_SETTINGSFILE" ]; then
   echo "ERROR: The webdanica settingsfile '$WEBDANICA_SETTINGSFILE' does not exist. Exiting program $PROG"
   exit 1
fi

if [ ! -f "$NAS_SETTINGSFILE" ]; then
   echo "WARNING: The netarchivesuite settingsfile '$NAS_SETTINGSFILE' does not exist. You may want to correct this in script $PROG"
fi



if [ ! -f "$WEBDANICA_TOOLS_JAR" ]; then
   echo "ERROR: The WEBDANICA_JAR '$WEBDANICA_TOOLS_JAR' does not exist. Maybe the lib folder is missing, or the webdanica VERSION is wrong. Exiting program $PROG"
   exit 1
fi

if [ ! -f "$PHOENIX_CLIENT_JAR" ]; then
   echo "ERROR: The PHOENIX_CLIENT_JAR '$PHOENIX_CLIENT_JAR' does not exist. Maybe the lib folder is missing, or the webdanica VERSION is wrong. Exiting program $PROG"
   exit 1
fi

CLASSPATH="$WEBDANICA_TOOLS_JAR:$PHOENIX_CLIENT_JAR:${SCRIPT_DIR}/lib/*"