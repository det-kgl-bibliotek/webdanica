#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

#Read version from pom.xml
VERSION=$(sed -E 's/xmlns="[^"]+"//g' pom.xml | xmllint --xpath '/project/version/text()' -)

HOST="webdanica@narcana-webdanica01.statsbiblioteket.dk"

set -e
pushd $SCRIPT_DIR > /dev/null


function rebuildProject(){

	echo "Rebuild the project if nessesary"

	function rebuild(){
		 mvn package -Psbprojects-nexus -DskipTests=true || exit 1
	}


	if [[ -e "${SCRIPT_DIR}/webdanica-webapp/target/webdanica-webapp-$VERSION.war" ]]; then
	#	If the war file exist, check if any src files or pom files have been modified later
		changedFiles=$(find \
			$SCRIPT_DIR/*/src \
			$SCRIPT_DIR/*/pom.xml \
			$SCRIPT_DIR/pom.xml \
			-type f \
			-newer $SCRIPT_DIR/webdanica-webapp/target/webdanica-webapp-$VERSION.war)

		if [ -n "$changedFiles" ]; then
			echo "rebuilding as changes in '$changedFiles'";
			rebuild
		fi
	else
		echo "war file not found, rebuilding"
		rebuild
	fi
}

function install_webapp(){
	local TOMCAT_VERSION="8.0.44"
	local TOMCAT_HOME="/opt/tomcat/apache-tomcat-$TOMCAT_VERSION"

	echo "Installing tomcat on $HOST"
	ssh -T $HOST <<-EOI
		set -x
		cd $(dirname $TOMCAT_HOME)

		wget -N "https://archive.apache.org/dist/tomcat/tomcat-$(echo $TOMCAT_VERSION | cut -d'.' -f1)/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
		[ -d apache-tomcat-$TOMCAT_VERSION ] || (tar -xvzf apache-tomcat-$TOMCAT_VERSION.tar.gz && rm -rf $TOMCAT_HOME/webapps/*)

		$TOMCAT_HOME/bin/shutdown.sh
		sleep 5

		ps -ef | grep ^webdani | grep tomcat | grep -v grep| sed 's/\s\+/ /g' | cut -d' ' -f2 | xargs -r -i kill {}
		[ -n "\$(ps -ef | grep ^webdani | grep tomcat | grep -v grep)" ] && (
			sleep 5;
			ps -ef | grep ^webdani | grep tomcat | grep -v grep| sed 's/\s\+/ /g' | cut -d' ' -f2 | xargs -r -i kill -9 {}
		)


		rm -rf $TOMCAT_HOME/logs/*

		mkdir -p $TOMCAT_HOME/shared
		ln -sf /usr/hdp/current/phoenix-client/phoenix-client.jar $TOMCAT_HOME/shared/phoenix-client.jar

		mkdir -p /opt/webdanica/harvestlogs
		set +x
	EOI

	echo "Installing config"
	rsync -avL "$SCRIPT_DIR/narcana/$TOMCAT_HOME/"* "$HOST:$TOMCAT_HOME"
	rsync -avL "$SCRIPT_DIR/narcana/home/"* "$HOST:/home/"

	echo "Uploading webapp"
	rsync -av "$SCRIPT_DIR/webdanica-webapp/target/webdanica-webapp-$VERSION.war" "$HOST:$TOMCAT_HOME/webapps/ROOT.war"


	ssh -T $HOST "$TOMCAT_HOME/bin/startup.sh"
	echo "Tomcat and Webdanica webapp installed"
}


function install_workflow(){
	echo "Installing automatic workflow"

	set -x
	local WORKFLOW_USER_HOME=/opt/workflows
	local AUTO_WORKFLOW_DIR="$WORKFLOW_USER_HOME/automatic-workflow/"

	rsync -av "$SCRIPT_DIR/webdanica-workflow/target/webdanica-workflow-$VERSION/" "$HOST:$AUTO_WORKFLOW_DIR"
	rsync -av "$SCRIPT_DIR/cronjobs" "$HOST:$WORKFLOW_USER_HOME"

	rsync -avL "$SCRIPT_DIR/narcana/$WORKFLOW_USER_HOME/" "$HOST:$WORKFLOW_USER_HOME/"

	set +x
	echo "Installed automatic workflow"

}

function install_tools(){
	echo "Installing CLI tools"

	set -x
	local TOOLS_USER_HOME=/opt/tools

	rsync -av "$SCRIPT_DIR/webdanica-tools/target/webdanica-tools-$VERSION/" "$HOST:$TOOLS_USER_HOME"

	rsync -avL "$SCRIPT_DIR/narcana/$TOOLS_USER_HOME/" "$HOST:$TOOLS_USER_HOME/"


	set +x
	echo "Installed automatic workflow"

}

function install_netarchivesuite(){
	echo "Installing Netarchivesuite"

	NAS_DIR=/opt/netarchivesuite
	NAS_VERSION=5.4


	rsync -avL "${SCRIPT_DIR}/narcana/$NAS_DIR/"* "$HOST:$NAS_DIR/"

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

rebuildProject
install_webapp
install_workflow
install_tools

#install_netarchivesuite
