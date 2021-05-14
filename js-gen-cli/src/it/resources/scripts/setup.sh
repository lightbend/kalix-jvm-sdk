#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Set up and install our npm-js tooling
pushd akkaserverless-npm-js

pushd akkasls-scripts
# Disable download of codegen CLI, and manually add our version
mv package.json original-package.json
node /home/scripts/disable-download-cli.js original-package.json > package.json
cp /home/akkasls-codegen-js bin/akkasls-codegen-js

# Use NPM link to make this available within the contianer
npm link
popd

pushd create-akkasls-entity
# Install create-akkasls-entity globally within the container
npm pack
npm i -g lightbend-create-akkasls-entity-1.0.0.tgz
popd

popd
