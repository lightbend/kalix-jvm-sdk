import sbt._
import sbt.Keys._
import sbt.Keys.streams
import sbtprotoc.ProtocPlugin
import ProtocPlugin.autoImport.PB
import akka.grpc.sbt.AkkaGrpcPlugin
import akka.grpc.sbt.AkkaGrpcPlugin.autoImport._
import sbt.internal.inc.classpath.ClasspathUtilities

import java.nio.file.{ Files, Path, Paths }

/**
 * A plugin that allows to use a code generator compiled in one subproject to be used in a test project
 */
object ReflectiveCodeGen extends AutoPlugin {

  override def requires = AkkaGrpcPlugin
  override def trigger = noTrigger

  val copyUnmanagedSources =
    settingKey[Boolean](
      "Flag to determine if code generation should copy generated unmanaged resources to the sourceDirectory directory if missing")

  override def projectSettings =
    Seq(
      Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
      Test / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
      Compile / sourceGenerators += runCodeGenTask.taskValue) ++ attachProtobufDescriptorSets

  def runAkkaServerlessCodegen(
      classpath: Classpath,
      protobufDescriptor: File,
      srcDir: File,
      genSrcDir: File,
      genTestSrcDir: File,
      logger: Logger): Seq[File] = {

    val cp = classpath.map(_.data)
    val loader = ClasspathUtilities.toLoader(cp, AkkaGrpcPlugin.getClass.getClassLoader)
    import scala.reflect.runtime.universe
    import scala.tools.reflect.ToolBox

    val tb = universe.runtimeMirror(loader).mkToolBox()

    val source =
      s"""
      |import com.lightbend.akkasls.codegen.java.SourceGenerator
      |import scala.collection.immutable
      |
      |(protobufDescriptor: java.io.File, srcDir: java.io.File, genSrcDir: java.io.File, genTestSrcDir: java.io.File, logger: sbt.util.Logger) => {
      |
      |  val path = genSrcDir.toPath
      |  val testPath = genTestSrcDir.toPath
      |
      |  implicit val codegenLog = new com.lightbend.akkasls.codegen.Log {
      |      override def debug(message: String): Unit = logger.debug(message)
      |      override def info(message: String): Unit = logger.info(message)
      |    }
      |
      |  SourceGenerator
      |    .generate(protobufDescriptor, srcDir.toPath, testPath, testPath, path, testPath, "com.example.Main")
      |    .map(_.toFile).to[immutable.Seq]
      |}
      """.stripMargin

    val generatorsF = tb.eval(tb.parse(source)).asInstanceOf[(File, File, File, File, sbt.util.Logger) => Seq[File]]
    generatorsF(protobufDescriptor, srcDir, genSrcDir, genTestSrcDir, logger)

  }

  lazy val runCodeGenTask =
    Def
      .task {
        val cp = (ProjectRef(file("."), "codegenJava") / Compile / fullClasspath).value
        val srcManaged = (Compile / sourceManaged).value
        val testSrcManaged = (Test / sourceManaged).value
        val tmpUnmanaged = (Compile / temporaryUnmanagedDirectory).value
        val sbtLogger = streams.value.log
        val generatedFiles =
          runAkkaServerlessCodegen(
            cp,
            protobufDescriptorSetOut.value,
            tmpUnmanaged,
            srcManaged,
            testSrcManaged,
            sbtLogger)

        if ((Compile / copyUnmanagedSources).value) // in this case use the files in the unmanaged source tree
          generatedFiles.filterNot(_.getCanonicalPath.startsWith(tmpUnmanaged.getCanonicalPath))
        else
          generatedFiles // use files directly from the
      }
      .dependsOn(Compile / PB.generate)

  lazy val protobufDescriptorSetOut = settingKey[File]("The file to write the protobuf descriptor set to")
  lazy val temporaryUnmanagedDirectory = settingKey[File]("Directory to generate 'unmanaged' sources into")
  val generateUnmanaged = taskKey[Seq[File]](
    "Generate \"unmanaged\" akkaserverless scaffolding code based on the available .proto definitions.\n" +
    "These are the source files that are placed in the source tree, and after initial generation should typically be maintained by the user.\n" +
    "Files that already exist they are not re-generated.")

  lazy val attachProtobufDescriptorSets = Seq(
    protobufDescriptorSetOut := (Compile / resourceManaged).value / "protobuf" / "descriptor-sets" / "user-function.desc",
    Compile / PB.generate := (Compile / PB.generate)
      .dependsOn(Def.task {
        protobufDescriptorSetOut.value.getParentFile.mkdirs()
      })
      .value,
    Compile / temporaryUnmanagedDirectory := (Compile / baseDirectory).value / "target" / "akkaserverless-unmanaged",
    Compile / PB.protocOptions ++= Seq(
      "--descriptor_set_out",
      protobufDescriptorSetOut.value.getAbsolutePath,
      "--include_source_info"),
    Compile / managedResources += protobufDescriptorSetOut.value,
    Compile / unmanagedResourceDirectories ++= (Compile / PB.protoSources).value,
    Compile / generateUnmanaged := {
      if ((Compile / copyUnmanagedSources).value) {
        Files.createDirectories(Paths.get((Compile / temporaryUnmanagedDirectory).value.toURI))
        // Make sure generation has happened
        val _ = (Compile / PB.generate).value
        val managed = (Compile / managedSources).value
        // Then copy over any new generated unmanaged sources
        copyIfNotExist(
          Paths.get((Compile / temporaryUnmanagedDirectory).value.toURI),
          Paths.get((Compile / sourceDirectory).value.toURI).resolve("java")
        ) // FIXME: copy java and scala separately
      } else Seq.empty
    },
    Compile / unmanagedSources :=
      (Compile / generateUnmanaged).value ++ (Compile / unmanagedSources).value)

  // copied from AkkaserverlessPlugin.scala
  private def copyIfNotExist(from: Path, to: Path): Seq[File] = {
    Files
      .walk(from)
      .filter(Files.isRegularFile(_))
      .flatMap[File](file => {
        val target = to.resolve(from.relativize(file))
        if (!Files.exists(target)) {
          Files.createDirectories(target.getParent)
          Files.copy(file, target)
          java.util.stream.Stream.of[File](target.toFile)
        } else java.util.stream.Stream.empty()
      })
      .toArray(new Array[File](_))
      .toVector
  }
}
