# ${artifactId}

#[[
## Designing
]]#
While designing your service it is useful to read [designing services](https://docs.kalix.io/services/development-process.html)

#[[
## Developing
]]#
This project has a bare-bones skeleton service ready to go, but in order to adapt and
extend it, it may be useful to read up on [developing services](https://docs.kalix.io/services/)
and in particular the [Java section](https://docs.kalix.io/java/index.html)

#[[
## Building
]]#
You can use Maven to build your project, which will also take care of
generating code based on the `.proto` definitions:

```shell
mvn compile
```

#[[
## Running Locally
]]#
In order to run your application locally, you must run the Kalix proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.
To start the proxy, run the following command from this directory:

```shell
docker-compose up
```

> Note: if you're looking to use Google Pub/Sub, see comments inside [docker-compose.yml](./docker-compose.yml) 
> on how to enable a Google Pub/Sub emulator that Kalix proxy will connect to.

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:exec
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:

```shell
> curl -XPOST -H "Content-Type: application/json" localhost:9000/${package}.MyServiceEntity/GetValue -d '{"entityId": "foo"}'
The command handler for `GetValue` is not implemented, yet
```

For example, using [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```shell
> grpcurl -plaintext -d '{"entityId": "foo"}' localhost:9000 ${package}.MyServiceEntity/GetValue
ERROR:
  Code: Unknown
  Message: The command handler for `GetValue` is not implemented, yet
```

> Note: The failure is to be expected if you have not yet provided an implementation of `GetValue` in
> your entity.

#[[
## Deploying
]]#
To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you use the `kalix` CLI to create a project as described in [Create a new Project](https://docs.kalix.io/projects/create-project.html). Once you have a project you can deploy your service into the project either
by using `mvn deploy` which will also conveniently package and publish your docker image prior to deployment,
or by first packaging and publishing the docker image through `mvn clean package docker:push -DskipTests` and
then [deploying the image through the `kalix` CLI](https://docs.kalix.io/services/deploy-service.html#_deploy).

