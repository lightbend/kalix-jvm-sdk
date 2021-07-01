import sbt._
import sbt.Keys._
import akka.grpc.sbt.AkkaGrpcPlugin
import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin}
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbtprotoc.ProtocPlugin
import scala.collection.breakOut

object CommonSettings extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings =
    Seq(
      organization := "com.akkaserverless",
      organizationName := "Lightbend Inc.",
      organizationHomepage := Some(url("https://lightbend.com")),
      homepage := Some(url("https://akkaserverless.com")),
      developers := List(
          Developer(id = "akka-serverless-team",
                    name = "Akka Serverless Team",
                    email = "info@lightbend.com",
                    url = url("https://lightbend.com"))
        ),
      scmInfo := Some(
          ScmInfo(
            url("https://github.com/lightbend/akkaserverless-java-sdk"),
            "scm:git@github.com:lightbend/akkaserverless-java-sdk.git"
          )
        ),
      startYear := Some(2021),
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      scalafmtOnCompile := true,
      scalaVersion := Dependencies.ScalaVersion,
      run / javaOptions ++= {
        sys.props.collect { case (key, value) if key.startsWith("akka") => s"-D$key=$value" }(breakOut)
      },
      // to get latest versions
      resolvers += "akka-http-snapshot-repository" at "https://oss.sonatype.org/content/repositories/snapshots",
    )

  override def projectSettings = Seq(
    run / fork := true,
    Test / fork := true,
    Test / javaOptions ++= Seq("-Xms1G")
  )
}

object CommonHeaderSettings extends AutoPlugin {

  override def requires = HeaderPlugin
  override def trigger = allRequirements

  import HeaderPlugin.autoImport._
  import de.heikoseeberger.sbtheader.FileType

  override def projectSettings = AutomateHeaderPlugin.projectSettings ++ Seq(
    headerMappings += FileType("proto") -> HeaderCommentStyle.cppStyleLineComment,
    headerSources / excludeFilter := (headerSources / excludeFilter).value || "package-info.java"
  )
}

object CommonAkkaGrpcSettings extends AutoPlugin {

  override def requires = AkkaGrpcPlugin
  override def trigger = allRequirements

  import AkkaGrpcPlugin.autoImport._
  import ProtocPlugin.autoImport._

  override def projectSettings = Seq(
    akkaGrpcCodeGeneratorSettings := Seq(), // remove `flat_package` setting added by Akka gRPC
    // protobuf external sources are filtered out by sbt-protoc and then added again by sbt-akka-grpc
    Compile / unmanagedResourceDirectories := (Compile / unmanagedResourceDirectories).value
        .filterNot(_ == PB.externalSourcePath.value),
    Test / unmanagedResourceDirectories := (Test / unmanagedResourceDirectories).value
        .filterNot(_ == PB.externalSourcePath.value)
  )
}
