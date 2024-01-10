/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.codegen.scalasdk

import kalix.Annotations
import kalix.codegen.scalasdk.impl.SourceGenerator
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature
import kalix.codegen.{ File, ModelBuilder }
import protocbridge.Artifact
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
