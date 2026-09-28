#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../server"
java -Xms2G -Xmx4G -jar purpur.jar --nogui
