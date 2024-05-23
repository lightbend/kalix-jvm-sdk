name := "eventsourced-customer-registry-subscriber"

organization := "io.kalix.samples"
organizationHomepage := Some(url("https://kalix.io"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "3.3.3"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

enablePlugins(KalixPlugin, JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/eclipse-temurin:21.0.2_13-jre-jammy"
// For Docker setup see https://docs.kalix.io/projects/container-registries.html
dockerRepository := sys.props.get("docker.registry").orElse(Some("kcr.us-east-1.kalix.io"))
dockerUsername := sys.props.get("docker.username") // use your Kalix organization name
dockerUpdateLatest := true
dockerBuildCommand := {
  val arch = sys.props("os.arch")
  if (arch != "amd64" && !arch.contains("x86")) {
    // use buildx with platform to build supported amd64 images on other CPU architectures
    // this may require that you have first run 'docker buildx create' to set docker buildx up
    dockerExecCommand.value ++ Seq(
      "buildx",
      "build",
      "--platform=linux/amd64",
      "--load") ++ dockerBuildOptions.value :+ "."
  } else dockerBuildCommand.value
}
ThisBuild / dynverSeparator := "-"

Compile / scalacOptions ++= Seq(
  "-release:21",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq(
  "-Xlint:unchecked",
  "-Xlint:deprecation",
  "-parameters" // for Jackson
)

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / javaOptions ++= Seq(
  // needed for the proxy to access the user function on all platforms
  "-Dkalix.user-function-interface=0.0.0.0",
  // the customer registry service is running at 8080, this service at 8081
  "-Dkalix.user-function-port=8081",
  "-Dlogback.configurationFile=logback-dev-mode.xml")
run / fork := true
Global / cancelable := false // ctrl-c

libraryDependencies ++= Seq(
  "io.kalix" %% "kalix-devtools" % KalixPlugin.KalixSdkVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test)
