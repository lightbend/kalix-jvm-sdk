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

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import protocgen.{ CodeGenApp, CodeGenRequest, CodeGenResponse }
import com.lightbend.akkasls.codegen.{ Log, ModelBuilder }

object AkkaserverlessGenerator extends CodeGenApp {
  val enableDebug = "enableDebug"

  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)
  }

  override def process(request: CodeGenRequest): CodeGenResponse = {
    val debugEnabled = request.parameter.contains(enableDebug)
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

  // FIXME #382 add reference to the runtime lib here
  override def suggestedDependencies: Seq[Artifact] = Nil
}
