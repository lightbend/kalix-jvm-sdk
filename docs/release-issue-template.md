# Release Kalix Java/Scala SDKs

### Prepare

- [ ] Make sure all important PRs have been merged
- [ ] Check that the [latest build](https://app.circleci.com/pipelines/github/lightbend/kalix-jvm-sdk) successfully finished
- [ ] Make sure a version of the proxy that supports the protocol version the SDK expects has been deployed to production

You can see the proxy version on prod [on grafana](https://lightbendcloud.grafana.net/d/ebzw4ARnz/prod-kalix-operations-dashboard?orgId=1) or using [various other methods](https://github.com/lightbend/kalix/wiki/Versioning-and-how-to-determine-what-version-is-running).

### Cutting the release 

- [ ] Edit the [draft release notes](https://github.com/lightbend/kalix-jvm-sdk/releases) and create the appropriate tag
    - CI will automatically publish to Sonatype OSSRH (Maven Central) based on the tag
    - CI will update the docs/kalix-current branch

### Check availability

- [ ] Check that [`docs/kalix-current`](https://github.com/lightbend/kalix-jvm-sdk/commits/docs/kalix-current) has been updated
- [ ] Check the release on [Maven Central](https://repo1.maven.org/maven2/io/kalix/kalix-scala-sdk-testkit_2.13/)

### Fix and publish docs

- [ ] In case there were important updates, update the [supported version in the main docs](https://github.com/lightbend/kalix-docs/blob/main/docs/modules/ROOT/partials/include.adoc#L21) (affects [Supported Languages](https://docs.kalix.io/reference/supported-languages.html))
- [ ] Add an item to the [Release Notes](https://github.com/lightbend/kalix-docs/blob/main/docs/modules/release-notes/pages/index.adoc) in the documentation
- [ ] Release the Kalix documentation to get the SDK docs updates published

### Update to the latest version
 
- [ ] Update the sdk version in samples and maven plugin
    - [ ] Update the `kalix-sdk.version` in the `samples/*/pom.xml` files to the released version.
    - [ ] Update the `kalix-sdk.version` default value in the `samples/scala-*/project/plugins.sbt` files to the release version
    - [ ] version in `maven-java/**/pom.xml`
 
### Check docs update

- [ ] Check for [updated docs](https://docs.kalix.io/index.html) (see the timestamp on the bottom of the page)

### Announcements

- [ ] Announce in [the forum](https://discuss.kalix.io/)
  - tag with releases and announcement
- [ ] Inform Lightbend marketing and engineering if it's a noteworthy release
- [ ] Close this issue
