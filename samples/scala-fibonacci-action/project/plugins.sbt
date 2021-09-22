addSbtPlugin("com.akkaserverless" % "sbt-akkaserverless" % System.getProperty("akkaserverless-sdk.version", "0.7.2"))
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.1.0") // FIXME should be included via sbt-akkaserverless
