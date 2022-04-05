name := "valueentity-shopping-cart"

organization := "io.kalix.samples"
organizationHomepage := Some(url("https://kalix.io"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "2.13.6"

enablePlugins(KalixPlugin, JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
dockerUpdateLatest := true
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
  sys.props += "kalix.user-function-interface" -> "0.0.0.0"
  (Compile / run).evaluated
}
run / fork := false
Global / cancelable := false // ctrl-c

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.7" % Test)
