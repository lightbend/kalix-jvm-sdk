#!/usr/bin/env bash
ALL_OK=true
for SAMPLE in samples/java* samples/scala*
do
  SAMPLE_NAME="${SAMPLE#samples/}"
  if [ $(grep -c ${SAMPLE_NAME} .github/workflows/ci.yml) -lt 1 ] ; then
    echo "${SAMPLE_NAME} is missing CI-tests"
    ALL_OK=false
  else
    echo "${SAMPLE_NAME} found"
  fi
done
$ALL_OK
