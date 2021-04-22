#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Re-generates sample proto file descriptors from the source
# Requires https://github.com/lightbend/akkaserverless-framework to be cloned locally, and its path provided
# Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework

if [ $# -eq 0 ]; then
    echo "No arguments supplied"
    echo "Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "$1 does not exist or is not a directory"
    exit 1
fi

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="$1/protocols/example/shoppingcart" \
--descriptor_set_out=event-sourced-shoppingcart.desc \
"$1/protocols/example/shoppingcart/persistence/domain.proto" \
"$1/protocols/example/shoppingcart/shoppingcart.proto"

echo "Generated event-sourced-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="$1/protocols/example/valueentity/shoppingcart" \
--descriptor_set_out=value-shoppingcart.desc \
"$1/protocols/example/valueentity/shoppingcart/persistence/domain.proto" \
"$1/protocols/example/valueentity/shoppingcart/shoppingcart.proto"

echo "Generated value-shoppingcart.desc"