# Releasing Akka Serverless Java SDK

1. Before releasing, update the [docs Makefile](https://github.com/lightbend/akkaserverless-java-sdk/blob/main/docs/Makefile#L38) to the new version that will be released.
2. Create a `vX.Y.Z` tag and release notes at https://github.com/lightbend/akkaserverless-java-sdk/releases
3. CircleCI will automatically publish to Sonatype OSSRH (Maven Central) based on the tag.
4. Update the `akkaserverless-sdk.version` in the `samples/*/pom.xml` files to the released version.
