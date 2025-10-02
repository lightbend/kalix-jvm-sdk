resolvers += "Akka repository".at("https://repo.akka.io/maven/github_actions")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.0.1")
// even updated `akka-grpc.version` in pom.xml files
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.5.7")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.7.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.12.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.11.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "3.0.2")

// force bumped because of CI not finding 1.0.0 which was the transitive version
addSbtPlugin("org.portable-scala" % "sbt-platform-deps" % "1.0.2")
