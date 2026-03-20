#!/bin/bash

VERSION=$1
if [ -z $VERSION ]
then
  echo "Specify the version to be released, e.g. 1.5.24"
else
  sed -e 's/\$VERSION\$/'$VERSION'/g' docs/release-issue-template.md > /tmp/release-$VERSION.md
  echo Created $(gh issue create --title "Release Kalix Java/Scala SDKs $VERSION" --label kalix-runtime --body-file /tmp/release-$VERSION.md --web)
fi
