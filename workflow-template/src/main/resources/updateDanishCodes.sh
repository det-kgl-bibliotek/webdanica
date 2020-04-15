#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

# replace path in HARVEST_SEEDS_HOME with the correct path
HARVEST_SEEDS_HOME=/home/test/automatic-workflow
OPTS1=-Ddk.netarkivet.settings.file=$HARVEST_SEEDS_HOME/conf/settings_NAS_Webdanica_staging.xml 
OPTS2=-Dwebdanica.settings.file=$HARVEST_SEEDS_HOME/conf/webdanica_settings.xml 
OPTS3=-Dlogback.configurationFile=$HARVEST_SEEDS_HOME/conf/silent_logback.xml 

#echo $(which java)

java $OPTS1 $OPTS2 $OPTS3 -cp "$SCRIPT_DIR/lib/webdanica-core-${project.version}.jar:*" dk.kb.webdanica.core.tools.UpdateDanishCodes
