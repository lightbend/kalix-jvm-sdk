## This example show how to create Spans by the users



## Running Locally


When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
sbt runAll
```

It's worth noting that `application.conf` is passing `kalix.telemetry.tracing.collector-endpoint="http://localhost:4317"`
to the application so the SDK knows where to export the traces. This is NOT needed when deploying in Kalix, only when run in local, that is, `mvn kalix:runAll`.

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.
This will also start a Jaeger service to which the services above will push the traces. You can find Jaeger at `http://localhost:16686`


With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java-protobuf/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`.
For example, using [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
grpcurl -plaintext localhost:9000  com.example.Controller/CallAsyncEndpoint                                 
```
produces
```
{
  "message": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",\n  \"body\": \"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"\n}"
}
```
or

```shell
grpcurl -plaintext localhost:9000  com.example.Controller/CallAsyncEndpoint  
```
produces
```
{
  "message": "{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",\n  \"body\": \"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"\n}"
}
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Install Kalix](https://docs.kalix.io/kalix/install-kalix.html)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you use the `kalix` CLI to create a project as described in [Create a new Project](https://docs.kalix.io/projects/create-project.html). Once you have a project you can deploy your service into the project either
by using `mvn deploy kalix:deploy` which will package, publish your docker image, and deploy your service to Kalix,
or by first packaging and publishing the docker image through `mvn deploy` and
then [deploying the image through the `kalix` CLI](https://docs.kalix.io/services/deploy-service.html#_deploy).