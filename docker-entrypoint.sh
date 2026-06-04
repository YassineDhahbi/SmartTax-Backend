#!/bin/sh
set -e

mkdir -p /app/uploads/users /app/uploads/publications /app/uploads/download-documents /app/logs
chown -R spring:spring /app/uploads /app/logs

JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"
exec runuser -u spring -- "$JAVA_BIN" -jar /app/app.jar
