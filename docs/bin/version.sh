#!/usr/bin/env bash
#
# Version for docs, based on nearest git tag.
# This is always a tagged/released version, not a dynamic version.

readonly prefix="v"
readonly tag=$(git describe --tags --abbrev=0 --match "$prefix[0-9]*" 2> /dev/null)
[ -n "$tag" ] && echo "${tag#$prefix}" || echo "0.0.0"
