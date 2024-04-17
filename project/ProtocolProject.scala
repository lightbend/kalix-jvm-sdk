import sbt._
import sbt.Keys._
import sbtprotoc.ProtocPlugin

/**
 * Settings for protocol projects.
 */
object ProtocolProject extends AutoPlugin {

  import ProtocPlugin.autoImport._

  override def requires = ProtocPlugin

  override def projectSettings =
    Seq(
      autoScalaLibrary := false,
      crossPaths := false,
      crossVersion := CrossVersion.disabled,
      libraryDependencies += "com.google.protobuf" % "protobuf-java" % Dependencies.ProtobufVersion % "protobuf",
      Compile / javacOptions ++= Seq("-encoding", "UTF-8"),
      Compile / compile / javacOptions ++= Seq("-source", "11", "-target", "11"),
      Compile / unmanagedResourceDirectories ++= (Compile / PB.protoSources).value
        .filterNot(_ == PB.externalSourcePath.value))
}

/**
 * Publicly published protocol project. Proto files are published as jars and as zip archives.
 */
object PublicProtocolProject extends AutoPlugin {
  object PublicProtocolProjectKeys {
    val packageZip = taskKey[File]("Create a zip archive for proto files")
  }

  val autoImport = PublicProtocolProjectKeys

  import autoImport._
  import ProtocPlugin.autoImport._

  override def requires = ProtocolProject && Publish

  override def projectSettings =
    Seq(
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      scmInfo := Some(
        ScmInfo(url("https://github.com/lightbend/kalix-jvm-sdk"), "scm:git@github.com:lightbend/kalix-jvm-sdk.git")),
      packageZip := {
        import com.typesafe.sbt.packager.universal.Archives
        val zipName = s"${name.value}-${version.value}"
        val mappings = (Compile / PB.protoSources).value.flatMap(Path.contentOf)
        Archives.makeNativeZip(target.value, zipName, mappings, top = Some(zipName), options = Seq.empty)
      },
      packageZip / artifact := Artifact(name.value, "zip", "zip")) ++ addArtifact(packageZip / artifact, packageZip)
}
