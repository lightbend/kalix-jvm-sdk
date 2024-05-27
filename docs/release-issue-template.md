# Release Kalix Java/Scala SDKs

### Prepare

- [ ] Make sure all important PRs have been merged
- [ ] Check that the [latest build](https://github.com/lightbend/kalix-jvm-sdk/actions?query=branch%3Amain) successfully finished
- [ ] Make sure a version of the Kalix Runtime that supports the protocol version the SDK expects has been deployed to production

You can see the Kalix Runtime version on prod [on grafana](https://grafana.sre.kalix.io/d/b30d0d8e-3894-4fbf-9627-9cb6088949ee/prod-kalix-metrics?orgId=1) or using [various other methods](https://github.com/lightbend/kalix/wiki/Versioning-and-how-to-determine-what-version-is-running).

### Cutting the release 

- [ ] Update the "Change date" on [the license](../blob/main/LICENSE#L9) to release date plus three years
- [ ] Use the "Generate release notes" button to create [a new release](https://github.com/lightbend/kalix-jvm-sdk/releases/new) with the appropriate tag.
    - Review the generated notes and "Publish release"
    - CI will automatically publish to the repository based on the tag
    - CI will update the docs/kalix-current branch

### Check availability

- [ ] Check that [`docs/kalix-current`](https://github.com/lightbend/kalix-jvm-sdk/commits/docs/kalix-current) has been updated
- [ ] Check the release on [Maven Central](https://repo1.maven.org/maven2/io/kalix/kalix-jvm-core-sdk/)

### Fix and publish docs

- [ ] In case there were important updates, update the [supported version in the main docs](https://github.com/lightbend/kalix-docs/blob/main/docs/modules/ROOT/partials/include.adoc#L21) (affects [Supported Languages](https://docs.kalix.io/sdk-support/supported-languages.html))
- [ ] Add an item to the [Release Notes](https://github.com/lightbend/kalix-docs/blob/main/docs/modules/release-notes/pages/index.adoc) in the documentation
- [ ] Release the Kalix documentation to get the SDK docs updates published

### Update to the latest version
 
- [ ] Review and merge PR created by bot (should appear [here](https://github.com/lightbend/kalix-jvm-sdk/pulls?q=is%3Apr+is%3Aopen+auto+pr+)). While reviewing confirm the release version is updated for:
    - `kalix-sdk.version` in the `samples/*/pom.xml` files
    - `kalix-sdk.version` default value in the `samples/scala-*/project/plugins.sbt` files 
    - version in `maven-java/**/pom.xml`
 
### Check docs update

- [ ] Check for [updated docs](https://docs.kalix.io/index.html) (see the timestamp on the bottom of the page)
- [ ] Check for the latest version on the [dependency configuration example](https://docs.kalix.io/java-protobuf/index.html#_reference_the_kalix_protobuf_sdks) 

### Announcements

- [ ] Add a summary of relevant changes and a link to the release notes into [Kalix Release Notes aggregation](https://docs.google.com/document/d/1Q0yWZssJHhF9oOKMW1yHq-QCyXJ-Ej8DeNuim4_QN6w/edit?usp=sharing)
- [ ] Close this issue
