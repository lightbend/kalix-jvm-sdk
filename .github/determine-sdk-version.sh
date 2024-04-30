#!/usr/bin/env bash
echo "Extracting version from sbt build"
# deliberately not using `--client`
sbt --no-colors "print coreSdk/version" | tail -n 1 | tr -d '\n' > ~/kalix-sdk-version.txt
# debugging help
hexdump -c ~/kalix-sdk-version.txt
