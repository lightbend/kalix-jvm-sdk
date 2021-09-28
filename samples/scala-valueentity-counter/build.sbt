name := "valueentity-counter"

organization := "com.akkaseverless.samples"
organizationHomepage := Some(url("https://akkaserverless.com"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "2.13.6"

enablePlugins(AkkaserverlessPlugin, JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters" // for Jackson
)

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

Compile / run := {
  // needed for the proxy to access the user function on all platforms
  sys.props += "akkaserverless.user-function-interface" -> "0.0.0.0"
  (Compile / run).evaluated
}
run / fork := false
Global / cancelable := false // ctrl-c

val LogbackVersion = "1.2.3"
val LogbackContribVersion = "0.1.5"
val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
val logbackJson = "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion
val logbackJackson = "ch.qos.logback.contrib" % "logback-jackson" % LogbackContribVersion

// FIXME include these dependencies via the AkkaserverlessPlugin
val AkkaServerlessSdkVersion = System.getProperty("akkaserverless-sdk.version", "0.7.2")
libraryDependencies ++= Seq(
  logback,
  logbackJson,
  logbackJackson,
  "org.scalatest" %% "scalatest" % "3.2.7" % Test,
  // FIXME include testkit dependency via the AkkaserverlessPlugin
  "com.akkaserverless" %% "akkaserverless-scala-sdk" % AkkaServerlessSdkVersion,
  "com.akkaserverless" %% "akkaserverless-scala-sdk-testkit" % AkkaServerlessSdkVersion % Test)

