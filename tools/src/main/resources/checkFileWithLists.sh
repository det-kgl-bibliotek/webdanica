#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))
PROG=$(readlink -f $BASH_SOURCE[0])
source $SCRIPT_DIR/common.sh


#java $OPTS1 $OPTS2 $OPTS3 -cp lib/webdanica-core-$VERSION.jar:lib/common-core-$NAS_VERSION.jar:lib/harvester-core-$NAS_VERSION.jar:lib/dom4j-1.6.1.jar:lib/jaxen-1.1.jar:lib/lucene-core-4.4.0.jar dk.kb.webdanica.core.tools.CheckListFileFormat $1
$JAVA_HOME/bin/java $OPTS1 $OPTS2 $OPTS3 -cp "$CLASSPATH" dk.kb.webdanica.core.tools.CheckListFileFormat $1
