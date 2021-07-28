# Releasing Akka Serverless Java SDK

1. Create a `vX.Y.Z` tag
  * git tag -a -s -m "Release 0.7.0-beta.16" v0.7.0-beta.16
  * git push --tags
2. CircleCI will automatically publish to Sonatype OSSRH (Maven Central) based on the tag.
3. If all seems well, create a [new release](https://github.com/akkaserverless-java-sdk/releases/new) for the new version.
