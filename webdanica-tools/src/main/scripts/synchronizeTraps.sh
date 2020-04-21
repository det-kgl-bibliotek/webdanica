#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

#This scripts has no arguments. It synchronizes the blacklists in hbase with the global crawlertraps in netarchivesuite.
#
#Currently, there is by default a maximum of 1000 characters on each trap found in hbase, because the default derby
# database has a max of 1000 characters.
#This default can be changed by setting the 'settings.crawlertraps.maxTrapSize' explicitly.
#
#Any trap exceeding this value or not valid xml is skipped during the synchronization.
#During synchronization, each blacklist in hbase will be created as a globalcrawlertrap in Netarchivesuite with the
# same name, and contents.
#If the globalcrawlertrap already exists in netarchivesuite, it will be updated.

$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH"  dk.kb.webdanica.core.interfaces.harvesting.SynchronizeCrawlertraps
