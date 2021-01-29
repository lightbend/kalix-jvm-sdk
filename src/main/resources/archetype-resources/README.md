# ${artifactId}

#[[
## Designing
]]#
While designing your service it is useful to read [designing Cloudstate services](https://developer.lightbend.com/docs/akka-serverless/designing/index.html)

#[[
## Developing
]]#
This project has a bare-bones skeleton service ready to go, but in order to adapt and
extend it it may be useful to read up on [developing Cloudstate services](https://developer.lightbend.com/docs/akka-serverless/developing/index.html)
and in particular the [Java section](https://developer.lightbend.com/docs/akka-serverless/java-services/index.html)

#[[
## Building
]]#
To build, at a minimum you need to generate sources, particularly when using an IDE:

```
mvn generate-sources
```

#[[
## Running Locally
]]#
In order to run your application locally, you must run the Cloudstate proxy. The included `docker-compose` file contains the configuration required to run the proxy for a locally running application. To start the proxy, run the following command from this directory:

```
docker-compose up -d
```

On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007), or for the `USER_FUNCTION_HOST` environment variable to be set manually.

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```
mvn exec:java
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. E.g.

```
> curl http://localhost:9000/${package}.MyEntity/myFirstEntityId
{"value":0}
```

#[[
## Deploying
]]#
To deploy your service, install the `akkasls` CLI as documented in
[Setting up a local development environment](https://developer.lightbend.com/docs/akka-serverless/getting-started/set-up-development-env.html)
and configure a Docker Registry to upload your docker image to.

You likely want to update the `akkasls.dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://developer.lightbend.com/docs/akka-serverless/deploying/registries.html)
for more information on how to make your docker image available to Akka Serverless.

Finally you can or use the [Akka Serverless Console](https://console.akkaserverless.com)
to create a project and then deploy your service into the project either through
the `akkasls` CLI or via the web interface.
