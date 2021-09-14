import sbt._
import sbt.Keys._
import sbt.Keys.streams

import sbtprotoc.ProtocPlugin
import ProtocPlugin.autoImport.PB
import akka.grpc.sbt.AkkaGrpcPlugin
import akka.grpc.sbt.AkkaGrpcPlugin.autoImport._
import sbt.internal.inc.classpath.ClasspathUtilities

/**
 * A plugin that allows to use a code generator compiled in one subproject to be used in a test project
 */
object ReflectiveCodeGen extends AutoPlugin {

  override def requires = AkkaGrpcPlugin
  override def trigger = noTrigger

  // FIXME cannot be repeatedly run, requires a clean in between generation runs for all classes to be found
  override def projectSettings =
    Seq(
      Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
      Test / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
      Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
      Compile / sourceGenerators += runCodeGenTask.taskValue) ++ attachProtobufDescriptorSets

  def runAkkaServerlessCodegen(
      classpath: Classpath,
      protobufDescriptor: File,
      genSrcDir: File,
      testSrcManaged: File,
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
      |(protobufDescriptor: java.io.File, genSrcDir: java.io.File, genTestSrcDir: java.io.File, logger: sbt.util.Logger) => {
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
      |    .generate(protobufDescriptor, path, testPath, testPath, path, testPath, "com.example.Main")
      |    .map(_.toFile).to[immutable.Seq]
      |}  
      """.stripMargin

    val generatorsF = tb.eval(tb.parse(source)).asInstanceOf[(File, File, File, sbt.util.Logger) => Seq[File]]
    generatorsF(protobufDescriptor, genSrcDir, testSrcManaged, logger)

  }

  lazy val runCodeGenTask =
    Def
      .task {
        val cp = (ProjectRef(file("."), "codegenJava") / Compile / fullClasspath).value
        val srcManaged = (Compile / sourceManaged).value
        val testSrcManaged = (Test / sourceManaged).value
        val sbtLogger = streams.value.log
        runAkkaServerlessCodegen(cp, protobufDescriptorSetOut.value, srcManaged, testSrcManaged, sbtLogger)
      }
      .dependsOn(Compile / PB.generate)

  lazy val protobufDescriptorSetOut = settingKey[File]("The file to write the protobuf descriptor set to")
  lazy val attachProtobufDescriptorSets = Seq(
    protobufDescriptorSetOut := (Compile / resourceManaged).value / "protobuf" / "descriptor-sets" / "user-function.desc",
    Compile / PB.generate := (Compile / PB.generate)
      .dependsOn(Def.task {
        protobufDescriptorSetOut.value.getParentFile.mkdirs()
      })
      .value,
    Compile / PB.protocOptions ++= Seq(
      "--descriptor_set_out",
      protobufDescriptorSetOut.value.getAbsolutePath,
      "--include_source_info"),
    Compile / managedResources += protobufDescriptorSetOut.value,
    Compile / unmanagedResourceDirectories ++= (Compile / PB.protoSources).value)
}
