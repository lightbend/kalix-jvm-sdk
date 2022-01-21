import sbt._
import sbt.Keys._
import java.io.File
import de.heikoseeberger.sbtheader.HeaderPlugin
import org.scalafmt.sbt.ScalafmtPlugin
import sbtprotoc.ProtocPlugin
import ProtocPlugin.autoImport.PB
import akka.grpc.sbt.AkkaGrpcPlugin
import akka.grpc.sbt.AkkaGrpcPlugin.autoImport._
import com.lightbend.sbt.JavaFormatterPlugin
object ExampleSuiteCompilationProject {

  def compilationProject(grpcTargetLang: AkkaGrpc.Language, pathToTests: String)(configureFunc: Project => Project) = {
    new CompositeProject {

      def componentProjects: Seq[Project] =
        // we only load the project if enabled by flag
        if (sys.props.contains(s"example.suite.$languagueLabel.enabled"))
          innerProjects :+ root
        else
          Seq.empty[Project]

      val languagueLabel = grpcTargetLang match {
        case AkkaGrpc.Scala => "scala"
        case AkkaGrpc.Java  => "java"
      }

      lazy val root =
        Project(id = s"codegen${languagueLabel.capitalize}CompilationExampleSuite", base = file(pathToTests))
          .aggregate(innerProjects.map(p => p: ProjectReference): _*)

      lazy val innerProjects =
        findProjects
          .map { case (dir, name) =>
            Project(s"test-$languagueLabel" + name, dir)
              .disablePlugins(HeaderPlugin, ScalafmtPlugin, JavaFormatterPlugin)
              .settings(
                Compile / unmanagedSourceDirectories ++= Seq("generated-managed", "generated-unmanaged").map(
                  baseDirectory.value / _),
                Test / unmanagedSourceDirectories ++= Seq("generated-test-managed", "generated-test-unmanaged").map(
                  baseDirectory.value / _),
                Compile / PB.protoSources += baseDirectory.value / "proto",
                akkaGrpcGeneratedLanguages := Seq(grpcTargetLang))
              .enablePlugins(ProtocPlugin, AkkaGrpcPlugin)
          }
          .map(configureFunc) // apply specific settings

      def findProjects: Seq[(File, String)] =
        findProjectsDir(file(pathToTests)).map { file =>
          val name = file.getPath.replace(pathToTests, "").replaceAll("[./]+", "-")
          (file, name)
        }

      def findProjectsDir(base: File): Seq[File] =
        if (base.listFiles().exists(d => d.isDirectory && d.getName == "proto")) Seq(base)
        else
          base
            .listFiles()
            .filter(f => f.isDirectory && f.getName != "." && f.getName != "..")
            .flatMap(findProjectsDir)
    }
  }
}
