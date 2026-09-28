#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../plugin"
mvn clean package
cp target/AmigosSMP.jar ../server/plugins/AmigosSMP.jar
echo "Plugin compilado em server/plugins/AmigosSMP.jar"
