import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbtdynver.DynVerPlugin.autoImport._

object DockerImage extends AutoPlugin {
  override def requires = JavaAppPackaging && DockerPlugin && SdkVersion

  override def projectSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk11:jre-11.0.8_10-ubi",
    dockerUsername := Some("kalix"),
    dockerUpdateLatest := true,
    // disable javadoc/scaladoc for projects published as docker images
    Compile / packageDoc / publishArtifact := false)
}

object LocalDockerImage extends AutoPlugin {
  override def requires = DockerImage
}

object PublicDockerImage extends AutoPlugin {
  override def requires = DockerImage

  override def projectSettings =
    Seq(dockerRepository := Some("gcr.io"), dockerUsername := Some("kalix-public"))
}
