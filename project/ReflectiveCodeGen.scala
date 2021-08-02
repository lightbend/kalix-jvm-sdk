import sbt._
import sbt.Keys._

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

  override def projectSettings =
    Seq(
      Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
      Test / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
      Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
      Compile / sourceGenerators += runCodeGenTask.taskValue
    ) ++ attachProtobufDescriptorSets

  def runAkkaServerlessCodegen(classpath: Classpath, protobufDescriptor: File, genSrcDir: File): Seq[File] = {

    val cp = classpath.map(_.data)
    val loader = ClasspathUtilities.toLoader(cp, AkkaGrpcPlugin.getClass.getClassLoader)
    import scala.reflect.runtime.universe
    import scala.tools.reflect.ToolBox

    val tb = universe.runtimeMirror(loader).mkToolBox()

    val source =
      s"""
      |import com.lightbend.akkasls.codegen.InternalCodegenPlugin
      |
      |(protobufDescriptor: java.io.File, genSrcDir: java.io.File) => InternalCodegenPlugin.runAkkaServerlessCodegen(protobufDescriptor, genSrcDir)
      """.stripMargin

    val generatorsF = tb.eval(tb.parse(source)).asInstanceOf[(File, File) => Seq[File]]
    generatorsF(protobufDescriptor, genSrcDir)

  }

  lazy val runCodeGenTask =
    Def
      .task {
        val cp = (ProjectRef(file("."), "codegenSbtPlugin") / Compile / fullClasspath).value
        runAkkaServerlessCodegen(cp, protobufDescriptorSetOut.value, (Compile / sourceManaged).value)
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
        "--include_source_info"
      ),
    Compile / managedResources += protobufDescriptorSetOut.value,
    Compile / unmanagedResourceDirectories ++= (Compile / PB.protoSources).value
  )
}
