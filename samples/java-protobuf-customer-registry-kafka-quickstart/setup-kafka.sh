#!/usr/bin/env bash

# This is script is used in CI to build java-protobuf-customer-registry-kafka-quickstart image.
#
# The CI script uses a matrix with a pre-cmd hook to run tasks before the tests.
# However, we can't pass more than one command in the pre-cmd hook. 
# To work it around, we pass a simple shell script with all the steps that we require for the pre-cmd hook.

# in CI we'll have a file kalix-sdk-version.txt at the home dir
docker-compose -f ../../.circleci/kafka.yml up -d
sleep 10
docker ps
docker network ls
docker run -t --network circleci_default confluentinc/cp-kafkacat kafkacat -b host.docker.internal:9092 -L -t customer_changes
docker run -t --network circleci_default confluentinc/cp-kafkacat kafkacat -b localhost:9093 -L -t customer_changes
docker run -t --network circleci_default confluentinc/cp-kafkacat kafkacat -b kafka:29092 -L -t customer_changes
docker run --rm --add-host host.docker.internal:host-gateway curlimages/curl:7.86.0 -L -v http://host.docker.internal:9093
docker run --rm --add-host host.docker.internal:host-gateway curlimages/curl:7.86.0 -L -v http://host.testcontainers.internal:9093
docker run --rm --add-host host.docker.internal:host-gateway curlimages/curl:7.86.0 -L -v http://host.docker.internal:9092
exit 0