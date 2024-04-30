organization := "com.example"

scalaVersion := "2.13.13"


enablePlugins(KalixPlugin, JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/eclipse-temurin:21.0.2_13-jre-jammy"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
dockerUpdateLatest := true
dockerBuildCommand := {
  val arch = sys.props("os.arch")
  if (arch != "amd64" && !arch.contains("x86")) {
    // use buildx with platform to build supported amd64 images on other CPU architectures
    // this may require that you have first run 'docker buildx create' to set docker buildx up
    dockerExecCommand.value ++ Seq("buildx", "build", "--platform=linux/amd64", "--load") ++ dockerBuildOptions.value :+ "."
  } else dockerBuildCommand.value
}
ThisBuild / dynverSeparator := "-"
run / fork := true
run / envVars += ("HOST", "0.0.0.0")
run / javaOptions ++= Seq("-Dlogback.configurationFile=logback-dev-mode.xml")

Compile / scalacOptions ++= Seq(
  "-release:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters" // for Jackson
)

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M11" % Compile,
  "org.json4s" %% "json4s-native" % "4.1.0-M5"% Compile,
  "org.scalatest" %% "scalatest" % "3.2.7" % Test
)
