#!/usr/bin/env bash
set -e

echo "Extracting version from sbt build"
sbt --client --no-colors "print coreSdk/version" > sdk-version-raw.txt
# debugging help
echo "----"
cat sdk-version-raw.txt
echo "----"
cat sdk-version-raw.txt | tail -n 3 | head -n 1 | sed $'s/\033\[[0-9;]*m//g' > sdk-version.txt
