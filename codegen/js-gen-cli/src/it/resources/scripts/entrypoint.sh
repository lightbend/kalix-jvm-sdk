#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Codegen container is running!"
echo "This process will continue to run until killed."
echo "Use Docker exec or equivalent methods to test the codegen library."
tail -f /dev/null
