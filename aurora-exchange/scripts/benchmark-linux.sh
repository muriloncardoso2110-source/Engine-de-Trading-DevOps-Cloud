#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -B package
java -XX:+UseG1GC -XX:MaxGCPauseMillis=10 -Xms512m -Xmx1g -jar target/aurora-exchange-1.0.0.jar benchmark 100000 10
