#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
JAR="$WRAPPER_DIR/gradle-wrapper.jar"
JAR_URL="https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar"
JAR_SHA="2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"

if [ ! -f "$JAR" ]; then
  mkdir -p "$WRAPPER_DIR"
  TMP="$JAR.tmp"
  echo "Gradle wrapper JAR is missing; downloading official Gradle 8.11.1 wrapper..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$JAR_URL" -o "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP" "$JAR_URL"
  else
    echo "ERROR: curl or wget is required for the one-time wrapper bootstrap." >&2
    exit 1
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL=$(sha256sum "$TMP" | awk '{print $1}')
    if [ "$ACTUAL" != "$JAR_SHA" ]; then
      rm -f "$TMP"
      echo "ERROR: Gradle wrapper checksum mismatch." >&2
      exit 1
    fi
  fi
  mv "$TMP" "$JAR"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi
exec "$JAVACMD" -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
