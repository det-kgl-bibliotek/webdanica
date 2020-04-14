#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

# replace path in HARVEST_SEEDS_HOME with the correct path
HARVEST_SEEDS_HOME=/home/test/automatic-workflow
OPTS1=-Ddk.netarkivet.settings.file=$HARVEST_SEEDS_HOME/conf/settings_NAS_Webdanica_staging.xml 
OPTS2=-Dwebdanica.settings.file=$HARVEST_SEEDS_HOME/conf/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$HARVEST_SEEDS_HOME/conf/silent_logback.xml 

#echo $(which java)

java $OPTS1 $OPTS2 $OPTS3 -cp "lib/webdanica-core-1.0.0.jar:$SCRIPT_DIR/lib/phoenix-4.7.0-HBase-1.1-client.jar:$SCRIPT_DIR/lib/commons-io-2.0.1.jar:$SCRIPT_DIR/lib/common-core-5.1.jar:$SCRIPT_DIR/lib/harvester-core-5.1.jar:$SCRIPT_DIR/lib/derbyclient-10.12.1.1.jar:$SCRIPT_DIR/lib/archive-core-5.1.jar:$SCRIPT_DIR/lib/jwat-common-1.0.4.jar:$SCRIPT_DIR/lib/json-simple-1.1.1.jar:$SCRIPT_DIR/lib/log4j-1.2.17.jar:$SCRIPT_DIR/lib/slf4j-log4j12-1.7.12.jar:$SCRIPT_DIR/lib/commons-lang-2.3.jar" dk.kb.webdanica.core.tools.UpdateDanishCodes
