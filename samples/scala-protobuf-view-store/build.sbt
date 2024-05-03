name := "store"

organization := "io.kalix.samples"
organizationHomepage := Some(url("https://kalix.io"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "2.13.14"

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
run / fork := true
run / envVars += ("HOST", "0.0.0.0")
run / javaOptions ++= Seq("-Dlogback.configurationFile=logback-dev-mode.xml")

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

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.12" % Test)
