#!/bin/sh
# Gradle start up script for POSIX
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then PRG="$link"; else PRG=`dirname "$PRG"`"/$link"; fi
done
APP_HOME=`dirname "$PRG"`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVACMD="${JAVA_HOME}/bin/java"
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
