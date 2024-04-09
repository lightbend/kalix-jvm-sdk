/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import kalix.Annotations
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature
import kalix.codegen.{ File, ModelBuilder }
import protocgen.{ CodeGenApp, CodeGenRequest, CodeGenResponse }

abstract class AbstractKalixGenerator extends CodeGenApp {
  val enableDebug = "enableDebug"
  def rootPackage(packageName: String) = s"rootPackage=$packageName"
  val rootPackageRegex = """rootPackage=([\w.]+)""".r
  def extractRootPackage(parameter: String): Option[String] =
    rootPackageRegex.findFirstMatchIn(parameter).map(found => found.group(1))

  override def registerExtensions(registry: ExtensionRegistry): Unit =
    Annotations.registerAllExtensions(registry)

  def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File]

  override def process(request: CodeGenRequest): CodeGenResponse = {
    val debugEnabled = request.parameter.contains(enableDebug)
    val model = ModelBuilder.introspectProtobufClasses(request.filesToGenerate, request.allProtos)(
      DebugPrintlnLog(debugEnabled),
      ProtoMessageTypeExtractor(request))
    try {
      CodeGenResponse.succeed(
        generateFiles(model, extractRootPackage(request.parameter))
          .map(file =>
            CodeGeneratorResponse.File
              .newBuilder()
              .setName(file.name)
              .setContent(file.content)
              .build()),
        Set(Feature.FEATURE_PROTO3_OPTIONAL))
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        CodeGenResponse.fail(t.getMessage)
    }
  }
}
