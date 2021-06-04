#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

pushd $1

mvn compile

popd
