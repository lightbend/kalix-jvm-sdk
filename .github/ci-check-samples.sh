#!/usr/bin/env bash
ALL_OK=true
for SAMPLE in samples/java*
do
  SAMPLE_NAME="${SAMPLE#samples/}"
  if [ $(grep -c ${SAMPLE_NAME} .github/workflows/samples.yml) -lt 1 ] ; then
    echo "${SAMPLE_NAME} is missing CI-tests"
    ALL_OK=false
  fi
done
$ALL_OK
