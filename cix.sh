#!/bin/bash
set -euo pipefail
echo "Running with SQ=$SQ_VERSION"

 
cd its
mvn -B -e -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false verify



