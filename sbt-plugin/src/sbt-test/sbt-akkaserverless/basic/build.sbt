scalaVersion := "2.13.6"

enablePlugins(AkkaserverlessPlugin)

val AkkaServerlessSdkVersion = System.getProperty("plugin.version", "0.7.2")
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.7" % Test,
  // FIXME include testkit dependency via the AkkaserverlessPlugin
  "com.akkaserverless" %% "akkaserverless-scala-sdk-testkit" % AkkaServerlessSdkVersion % Test)
