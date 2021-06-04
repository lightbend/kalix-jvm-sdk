#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Re-generates sample proto file descriptors from the source
# Requires https://github.com/lightbend/akkaserverless-java-sdk to be cloned locally, and its path provided
# Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework /path/to/akkaserverless-java-sdk

if [ $# -lt 2 ]; then
    echo "Required arguments not supplied"
    echo "Usage: ./generate-from-samples.sh /path/to/akkaserverless-framework /path/to/akkaserverless-java-sdk"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "$1 does not exist or is not a directory"
    exit 1
fi

if [ ! -d "$2" ]; then
    echo "$2 does not exist or is not a directory"
    exit 1
fi
protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="$2/samples/java-eventsourced-shopping-cart/src/main/proto/shoppingcart" \
--descriptor_set_out=event-sourced-shoppingcart.desc \
"$2/samples/java-eventsourced-shopping-cart/src/main/proto/shoppingcart/shoppingcart_domain.proto" \
"$2/samples/java-eventsourced-shopping-cart/src/main/proto/shoppingcart/shoppingcart_api.proto"

echo "Generated event-sourced-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="$2/samples/java-valueentity-shopping-cart/src/main/proto/shoppingcart" \
--descriptor_set_out=value-shoppingcart.desc \
"$2/samples/java-valueentity-shopping-cart/src/main/proto/shoppingcart/shoppingcart_domain.proto" \
"$2/samples/java-valueentity-shopping-cart/src/main/proto/shoppingcart/shoppingcart_api.proto"

echo "Generated value-shoppingcart.desc"

protoc --include_imports \
--proto_path="$1/protocols/sdk/src/main/protobuf" \
--proto_path="$2/samples/java-eventing-shopping-cart/src/main/proto" \
--proto_path="$2/samples/java-eventing-shopping-cart/src/main/proto/cart/shopping_cart_domain.proto" \
--descriptor_set_out=view-shoppingcart.desc \
"$2/samples/java-eventing-shopping-cart/src/main/proto/cart/shopping_cart_view_model.proto"

echo "Generated view-shoppingcart.desc"