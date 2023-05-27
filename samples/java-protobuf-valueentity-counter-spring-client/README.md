# Implementing Spring Client for Counter as a Value Entity

This project/module is a spring client to java-protobuf-valueentity-counter module exposed endpoints via REST and GRPC.
This service exposes REST API endpoints and underneath calls relevant API's in java entity counter service.

It's dependent on java-protobuf-valueentity-counter module.

## Building and running unit tests

To compile and test the code from the command line, use

```shell
mvn verify
```

## Running Locally

In order to run your service locally, please ensure that module is running in your local setup java-protobuf-valueentity-counter.
By default, this service connects to java-protobuf-valueentity-counter running on localhost and port 9000. This is configurable in
``application.properties`` file with properties ``as.host`` and ``as.host``

To start your service locally, run:

```shell
mvn spring-boot:run
```

## Exercise the service

With both the java-protobuf-valueentity-counter module and your service running, any defined endpoints should be available at `http://localhost:8083`.
Rest Endpoints are exposed via this service, and they can be invoked using any RestClient.

```shell
curl --request GET 'localhost:8083/counter/foo'
```

```shell
curl --request POST 'localhost:8083/counter/foo/increase' \
--header 'Content-Type: application/json' \
--data-raw '{"counterId": "foo", "value" : 1}'
```

```shell
curl --request POST 'localhost:8083/counter/foo/decrease' \
--header 'Content-Type: application/json' \
--data-raw '{"counterId": "foo", "value" : 1}'
```

```shell
curl --request POST 'localhost:8083/counter/foo/reset'
```
