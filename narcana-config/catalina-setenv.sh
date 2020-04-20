JAVA_HOME=/usr/lib/jvm/java-1.8.0
WEBDANICA_HOME=/home/webdanica/webdanica-home
export WEBDANICA_HOME JAVA_HOME

export CATALINA_OPTS="$CATALINA_OPTS -Xms512m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx7g"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=256m"


