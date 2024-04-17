resolvers += "Akka library repository".at("https://repo.akka.io/maven")

addSbtPlugin("io.kalix" % "sbt-kalix" % System.getProperty("kalix-sdk.version", "1.4.1"))
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

// for protobuf validation
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11",
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.4")
