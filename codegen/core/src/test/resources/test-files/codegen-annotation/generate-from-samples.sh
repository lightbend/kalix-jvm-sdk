#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Re-generates sample proto file descriptors from the source
# Requires https://github.com/lightbend/akkaserverless-java-sdk to be cloned locally, and its path provided
# Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework /path/to/akkaserverless-java-sdk

if [ $# -lt 1 ]; then
    echo "Required arguments not supplied"
    echo "Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "$1 does not exist or is not a directory"
    exit 1
fi


protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/event-sourced-entity-unnamed" \
--descriptor_set_out=descriptor-sets/event-sourced-shoppingcart-unnamed.desc \
"protos/event-sourced-entity-unnamed/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/event-sourced-entity-unnamed/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated event-sourced-shoppingcart-unnamed.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/event-sourced-entity" \
--descriptor_set_out=descriptor-sets/event-sourced-shoppingcart.desc \
"protos/event-sourced-entity/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/event-sourced-entity/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated event-sourced-shoppingcart.desc"


protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/view" \
--descriptor_set_out=descriptor-sets/view-shoppingcart.desc \
"protos/view/com/example/shoppingcart/view/shopping_cart_view_model.proto"

echo "Generated view-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/view-named" \
--descriptor_set_out=descriptor-sets/view-shoppingcart-named.desc \
"protos/view-named/com/example/shoppingcart/view/shopping_cart_view_model.proto"

echo "Generated view-shoppingcart-named.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/value-entity-unnamed" \
--descriptor_set_out=descriptor-sets/value-shoppingcart-unnamed.desc \
"protos/value-entity-unnamed/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/value-entity-unnamed/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated value-shoppingcart-unnamed.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/value-entity" \
--descriptor_set_out=descriptor-sets/value-shoppingcart.desc \
"protos/value-entity/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/value-entity/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated value-shoppingcart.desc"


protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/action" \
--descriptor_set_out=descriptor-sets/action-shoppingcart.desc \
"protos/action/com/example/shoppingcart/shoppingcart_controller_api.proto"

echo "Generated action-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/action-named" \
--descriptor_set_out=descriptor-sets/action-shoppingcart-named.desc \
"protos/action-named/com/example/shoppingcart/shoppingcart_controller_api.proto"

echo "Generated action-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/replicated-entity-unnamed" \
--descriptor_set_out=descriptor-sets/replicated-shoppingcart-unnamed.desc \
"protos/replicated-entity-unnamed/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/replicated-entity-unnamed/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated replicated-shoppingcart-unnamed.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="protos/replicated-entity" \
--descriptor_set_out=descriptor-sets/replicated-shoppingcart.desc \
"protos/replicated-entity/com/example/shoppingcart/domain/shoppingcart_domain.proto" \
"protos/replicated-entity/com/example/shoppingcart/shoppingcart_api.proto"

echo "Generated replicated-shoppingcart.desc"
