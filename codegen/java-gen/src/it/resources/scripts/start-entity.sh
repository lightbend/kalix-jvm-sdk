#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

pushd $1

mvn exec:java > entity-logs.txt 2>&1 &
echo $! > .akkasls-pid

popd
