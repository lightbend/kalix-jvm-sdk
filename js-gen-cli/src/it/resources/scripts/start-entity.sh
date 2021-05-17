#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

pushd $1

npm run start &
echo $! > .akkasls-pid

popd
