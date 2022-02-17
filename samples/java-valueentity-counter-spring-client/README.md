# Implementing Spring Client for Counter as a Value Entity

This project/module is a spring client to java-valueentity-counter module exposed endpoints via REST and GRPC.
This application exposes REST API endpoints and underneath calls relevant API's in java entity counter application.


It's dependent on java-valueentity-counter module.


## Building and running unit tests

To compile and test the code from the command line, use

```shell
mvn verify
```

## Running Locally

In order to run your application locally, please ensure that module is running in your local setup java-valueentity-counter.
By default, this application connects to java-valueentity-counter running on localhost and port 9000. This is configurable in
``application.properties`` file with properties ``as.host`` and ``as.host``

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```
mvn spring-boot:run
```

## Exercise the service

With both the java-valueentity-counter module and your application running, any defined endpoints should be available at `http://localhost:8083`.
Rest Endpoints are exposed via this application, and they can be invoked using any RestClient.
```shell
curl --request POST 'localhost:8083/getCurrentCounter' \
--header 'Content-Type: application/json' \
--data-raw '{"counterId": "foo"}'
```