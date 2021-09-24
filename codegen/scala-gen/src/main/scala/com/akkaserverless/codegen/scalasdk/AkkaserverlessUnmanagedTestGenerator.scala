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
import com.lightbend.akkasls.codegen.ModelBuilder
import protocbridge.Artifact
import protocgen.CodeGenApp
import protocgen.CodeGenRequest
import protocgen.CodeGenResponse

object AkkaserverlessUnmanagedTestGenerator extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Annotations.registerAllExtensions(registry)
  }

  override def process(request: CodeGenRequest): CodeGenResponse = {
    val debugEnabled = request.parameter.contains(AkkaserverlessGenerator.enableDebug)
    val model = ModelBuilder.introspectProtobufClasses(request.filesToGenerate)(
      DebugPrintlnLog(debugEnabled),
      FullyQualifiedNameExtractor(request))
    try {
      CodeGenResponse.succeed(
        SourceGenerator
          .generateUnmanagedTest(model)
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

  override def suggestedDependencies: Seq[Artifact] = Nil
}
