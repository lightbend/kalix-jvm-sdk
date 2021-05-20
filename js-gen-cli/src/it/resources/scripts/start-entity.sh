#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

pushd $1
node src/index.js &
echo $! > .akkasls-pid

popd
