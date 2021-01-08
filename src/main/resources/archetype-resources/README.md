# ${artifactId}

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

To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```
mvn exec:java
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. E.g.

```
> curl http://localhost:9000/${package}.MyEntity/myFirstEntityId
{"value":0}
```