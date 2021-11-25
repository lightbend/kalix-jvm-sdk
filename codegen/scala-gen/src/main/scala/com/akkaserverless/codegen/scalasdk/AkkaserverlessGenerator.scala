/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.codegen.scalasdk

import com.akkaserverless.Annotations
import com.akkaserverless.codegen.scalasdk.impl.SourceGenerator
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import protocgen.{ CodeGenApp, CodeGenRequest, CodeGenResponse }
import com.lightbend.akkasls.codegen.{ Log, ModelBuilder }
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

object AkkaserverlessGenerator extends CodeGenApp {
  val enableDebug = "enableDebug"
  def rootPackage(packageName: String) = s"rootPackage=$packageName"
  val rootPackageRegex = """rootPackage=(\w+)""".r
  def extractRootPackage(parameter: String): Option[String] =
    rootPackageRegex.findFirstMatchIn(parameter).map(found => found.group(1))

  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Annotations.registerAllExtensions(registry)
  }

  override def process(request: CodeGenRequest): CodeGenResponse = {
    val debugEnabled = request.parameter.contains(enableDebug)
    val configuredRootPackage = extractRootPackage(request.parameter)
    val model = ModelBuilder.introspectProtobufClasses(request.filesToGenerate)(
      DebugPrintlnLog(debugEnabled),
      FullyQualifiedNameExtractor(request))
    try {
      CodeGenResponse.succeed(
        SourceGenerator
          .generateManaged(model, configuredRootPackage)
          .map(file =>
            CodeGeneratorResponse.File
              .newBuilder()
              .setName(file.name)
              .setContent(file.content)
              .build()))
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        CodeGenResponse.fail(t.getMessage)
    }
  }

  // FIXME #382 add reference to the runtime lib here
  override def suggestedDependencies: Seq[Artifact] = Seq(
    Artifact(
      BuildInfo.organization,
      // FIXME determine scala version properly
      "akkaserverless-scala-sdk" + "_2.13",
      BuildInfo.version))
}
