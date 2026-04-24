#!/bin/sh
app_path=$0
while [ -h "$app_path" ]; do
    ls=$(ls -ld "$app_path")
    link=${ls#*' -> '}
    case $link in
      /*)   app_path=$link ;;
      *)    app_path=$(dirname "$app_path")/$link ;;
    esac
done
APP_HOME=$(cd "$(dirname "$app_path")" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"
else JAVACMD=java; fi
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "\"-Dorg.gradle.appname=$(basename $0)\"" \
  -classpath "\"$CLASSPATH\"" \
  org.gradle.wrapper.GradleWrapperMain "$@"
exec "$JAVACMD" "$@"
