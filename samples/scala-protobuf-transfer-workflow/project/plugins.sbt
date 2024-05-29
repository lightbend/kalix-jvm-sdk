resolvers += "Akka library repository".at("https://repo.akka.io/maven")
resolvers += "Akka library repository snapshots".at("https://repo.akka.io/snapshots")

addSbtPlugin("io.kalix" % "sbt-kalix" % System.getProperty("kalix-sdk.version", "1.5.0-10-a4513862-dev"))
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
