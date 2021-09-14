import sbt._
import sbt.Keys._
import akka.grpc.sbt.AkkaGrpcPlugin
import com.lightbend.sbt.JavaFormatterPlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin
import org.scalafmt.sbt.ScalafmtPlugin
import sbtprotoc.ProtocPlugin

/**
 * Add all the default scoped settings for integration tests.
 */
object IntegrationTests extends AutoPlugin {

  override def requires = plugins.JvmPlugin && CommonSettings && CommonHeaderSettings

  override def projectConfigurations = Seq(IntegrationTest)

  override def projectSettings =
    Defaults.itSettings ++
    inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings) ++
    inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings) ++
    HeaderPlugin.autoImport.headerSettings(IntegrationTest) ++
    AutomateHeaderPlugin.autoImport.automateHeaderSettings(IntegrationTest) ++ Seq(
      IntegrationTest / fork := (Test / fork).value)
}

/**
 * Automatically add integration test gRPC settings for AkkaGrpcPlugin + IntegrationTests.
 */
object AkkaGrpcIntegrationTests extends AutoPlugin {

  override def requires = AkkaGrpcPlugin && IntegrationTests
  override def trigger = allRequirements

  import ProtocPlugin.autoImport._

  override def projectSettings = AkkaGrpcPlugin.configSettings(IntegrationTest) ++ Seq(
    // protobuf external sources are filtered out by sbt-protoc and then added again by sbt-akka-grpc
    IntegrationTest / unmanagedResourceDirectories := (IntegrationTest / unmanagedResourceDirectories).value
      .filterNot(_ == PB.externalSourcePath.value))
}
