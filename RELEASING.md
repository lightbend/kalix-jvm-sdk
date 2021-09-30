# Releasing Akka Serverless Java SDK

1. Create a `vX.Y.Z` tag and release notes at https://github.com/lightbend/akkaserverless-java-sdk/releases
2. CircleCI will automatically publish to Sonatype OSSRH (Maven Central) based on the tag.
3. Update the `akkaserverless-sdk.version` in the `samples/*/pom.xml` files to the released version.
4. Update the `akkaserverless-sdk.version` default value in the `samples/scala-*/project/plugins.sbt` files to the release version.


## Publishing documentation hotfixes

Docs will be published automatically on release. Docs can also be published manually for hotfixes.

The version used in the docs will be the nearest tag. If all doc changes since the last release should be published, run (in the `docs` dir, or with `-C docs`):

```
make deploy
```

If only some doc changes are needed, branch from the last release tag, cherry-pick the needed doc changes, and then run `make deploy`.

This will publish the doc sources to the `docs/current` branch. They will be included automatically in the next build for the main docs. A build for the main docs can also be triggered by re-running the last docs build in CircleCI (on the `master` branch for dev docs, on the `current` branch for prod docs).
