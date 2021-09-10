# Releasing Akka Serverless Java SDK

1. Create a `vX.Y.Z` tag and release notes at https://github.com/lightbend/akkaserverless-java-sdk/releases
2. CircleCI will automatically publish to Sonatype OSSRH (Maven Central) based on the tag.
3. Update the `akkaserverless-sdk.version` in the `samples/*/pom.xml` files to the released version.
