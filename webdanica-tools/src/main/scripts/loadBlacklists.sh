#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

#This script adds a new active blacklist to our webdanica workflow.

#We currently don't support updating or deleting a blacklist using this script.

#The current procedure is to erase all blacklists using the Apache phoenix CLI client 'sqlline.py' part of the phoenix-bin package 'apache-phoenix-PHOENIXVERSION-HBase-HBASEVERSION-bin.tar.gz'

#(currently PHOENIXVERSION 5.0.0, and HBASEVERSION 2.0) with command "delete from blacklists;"


$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH"  dk.kb.webdanica.core.tools.LoadBlacklists $1
