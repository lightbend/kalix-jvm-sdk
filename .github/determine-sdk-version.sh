#!/usr/bin/env bash
set -e

echo "Extracting version from sbt build"
# deliberately not using `--client`
sbt --no-colors "print coreSdk/version" > sdk-version-raw.txt
# debugging help
echo "----"
cat sdk-version-raw.txt
echo "----"
cat sdk-version-raw.txt | tail -n 3 | head -n 1 | tr -d '\n'| tr -d '[:space:]' > sdk-version.txt
# debugging help
echo "----"
hexdump -c sdk-version.txt
echo "----"
