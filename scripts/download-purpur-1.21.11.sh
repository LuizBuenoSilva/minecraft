#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
curl -L "https://api.purpurmc.org/v2/purpur/1.21.11/latest/download" -o "$ROOT/server/purpur.jar"
echo "Purpur baixado para server/purpur.jar"
