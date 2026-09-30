#!/usr/bin/env bash

DIRNAME=$(dirname "$0")
if [ -z "$DIRNAME" ]; then
    DIRNAME="."
fi

unset ANDROID_PREFS_ROOT

if [ -d "/home/mark/Documents/Apps/android-studio-quail2-patch1-linux/android-studio/jbr" ]; then
    export JAVA_HOME="/home/mark/Documents/Apps/android-studio-quail2-patch1-linux/android-studio/jbr"
fi

JAVA_CMD="java"
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

exec "$JAVA_CMD" -cp "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
