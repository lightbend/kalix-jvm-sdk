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

  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Annotations.registerAllExtensions(registry)
  }

  override def process(request: CodeGenRequest): CodeGenResponse = {
    val debugEnabled = request.parameter.contains(enableDebug)
    implicit val di: DescriptorImplicits = descriptorImplicits(request)
    val model = ModelBuilder.introspectProtobufClasses(request.filesToGenerate)(DebugPrintlnLog(debugEnabled))
    try {
      CodeGenResponse.succeed(
        SourceGenerator
          .generateManaged(model)
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

  def descriptorImplicits(request: CodeGenRequest): DescriptorImplicits = {
    val params =
      request.parameter.split(",").map(_.trim).filter(_.nonEmpty).foldLeft[GeneratorParams](GeneratorParams()) {
        case (p, "java_conversions")            => p.copy(javaConversions = true)
        case (p, "flat_package")                => p.copy(flatPackage = true)
        case (p, "single_line_to_string")       => p.copy(singleLineToProtoString = true) // for backward-compatibility
        case (p, "single_line_to_proto_string") => p.copy(singleLineToProtoString = true)
        case (p, "ascii_format_to_string")      => p.copy(asciiFormatToString = true)
        case (p, "no_lenses")                   => p.copy(lenses = false)
        case (p, "retain_source_code_info")     => p.copy(retainSourceCodeInfo = true)
        case (p, "grpc")                        => p.copy(grpc = true)
        case (x, _)                             => x
      }
    DescriptorImplicits.fromCodeGenRequest(params, request)
  }

  // FIXME #382 add reference to the runtime lib here
  override def suggestedDependencies: Seq[Artifact] = Seq(
    Artifact(
      BuildInfo.organization,
      // FIXME determine scala version properly
      "akkaserverless-scala-sdk" + "_2.13",
      BuildInfo.version))
}
