#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

VERSION=2.2-SNAPSHOT
set -e

pushd $SCRIPT_DIR > /dev/null

echo "Rebuild the project if nessesary"

[ -e "${SCRIPT_DIR}/webdanica-webapp/target/webdanica-webapp-$VERSION.war" ] || \
	( (mvn --quiet clean package -Psbprojects-nexus -DskipTests=true) || exit 1 )

HOST="webdanica@narcana-webdanica01.statsbiblioteket.dk"

function install_webapp(){
	TOMCAT_VERSION="8.0.44"
	TOMCAT_HOME="/opt/tomcat/apache-tomcat-$TOMCAT_VERSION"

	echo "Installing tomcat on $HOST"
	ssh -T $HOST <<-EOI
		set -x
		cd $(dirname $TOMCAT_HOME)

		wget -N "https://archive.apache.org/dist/tomcat/tomcat-$(echo $TOMCAT_VERSION | cut -d'.' -f1)/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
		[ -d apache-tomcat-$TOMCAT_VERSION ] || (tar -xvzf apache-tomcat-$TOMCAT_VERSION.tar.gz && rm -rf $TOMCAT_HOME/webapps/*)

		$TOMCAT_HOME/bin/shutdown.sh
		sleep 5

		rm -rf $TOMCAT_HOME/logs/*

		mkdir -p $TOMCAT_HOME/shared
		ln -sf /usr/hdp/current/phoenix-client/phoenix-client.jar $TOMCAT_HOME/shared/phoenix-client.jar

		mkdir -p /opt/webdanica/harvestlogs
		set +x
	EOI

	echo "Installing config"
	rsync -av "$SCRIPT_DIR/narcana/$TOMCAT_HOME/"* "$HOST:$TOMCAT_HOME"
	rsync -av "$SCRIPT_DIR/narcana/home/"* "$HOST:/home/"

	echo "Uploading webapp"
	rsync -av "$SCRIPT_DIR/webdanica-webapp/target/webdanica-webapp-$VERSION.war" "$HOST:$TOMCAT_HOME/webapps/ROOT.war"


	ssh -T $HOST "$TOMCAT_HOME/bin/startup.sh"
	echo "Tomcat and Webdanica webapp installed"
}


function install_workflow(){
	echo "Installing automatic workflow"

	set -x
	WORKFLOW_USER_HOME=/opt/workflows
	AUTO_WORKFLOW_DIR="$WORKFLOW_USER_HOME/automatic-workflow/"

	rsync -av "$SCRIPT_DIR/workflow-template/target/workflow-template-$VERSION/" "$HOST:$AUTO_WORKFLOW_DIR"
	rsync -av "$SCRIPT_DIR/scripts/cronjobs" "$HOST:$WORKFLOW_USER_HOME"

	rsync -av "$SCRIPT_DIR/narcana/$WORKFLOW_USER_HOME/" "$HOST:$WORKFLOW_USER_HOME/"

	set +x
	echo "Installed automatic workflow"

}

function install_netarchivesuite(){
	echo "Installing Netarchivesuite"

	NAS_DIR=/opt/netarchivesuite
	NAS_VERSION=5.4


	rsync -av "${SCRIPT_DIR}/narcana/$NAS_DIR/"* "$HOST:$NAS_DIR/"

	ssh -T -A $HOST <<-EOI
	set -x

	cd "$NAS_DIR"
	wget -N "https://raw.githubusercontent.com/netarchivesuite/netarchivesuite/master/deploy/deploy-core/scripts/openmq/mq.sh"
	chmod a+x mq.sh
	[ -d "MessageQueue5.1" ] || "$NAS_DIR/mq.sh" install

	wget -N "https://sbforge.org/nexus/content/groups/public/org/netarchivesuite/distribution/$NAS_VERSION/distribution-$NAS_VERSION.zip"
	wget -N "https://sbforge.org/nexus/content/groups/public/org/netarchivesuite/heritrix3-bundler/$NAS_VERSION/heritrix3-bundler-$NAS_VERSION.zip"

	wget -N "https://raw.githubusercontent.com/netarchivesuite/netarchivesuite/master/deploy/deploy-core/scripts/RunNetarchiveSuite.sh"
	chmod +x RunNetarchiveSuite.sh

	./RunNetarchiveSuite.sh "distribution-$NAS_VERSION.zip" "deploy_narcana_netarchivesuite.xml" "deploy" "heritrix3-bundler-$NAS_VERSION.zip"

	EOI
}

install_webapp
install_workflow

#install_netarchivesuite
