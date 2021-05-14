#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

pushd $1

npm link @lightbend/akkasls-scripts
npm install
npm run build

popd
