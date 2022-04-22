#!/usr/bin/env bash
#
# This script configures docker to be able to push to Google Container Registry.
# It uses the service account associated with PUBLIC_GCR_SERVICE_KEY.

set -euo pipefail

echo "$KALIX_PUBLIC_GCR_SERVICE_KEY" | docker login -u _json_key --password-stdin https://gcr.io
