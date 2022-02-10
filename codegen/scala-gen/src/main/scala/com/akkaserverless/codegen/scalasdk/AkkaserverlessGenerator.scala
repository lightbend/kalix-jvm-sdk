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

import com.akkaserverless.codegen.scalasdk.impl.SourceGenerator
import protocbridge.Artifact
import com.lightbend.akkasls.codegen.{ File, ModelBuilder }

object AkkaserverlessGenerator extends AbstractAkkaserverlessGenerator {

  override def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] =
    SourceGenerator.generateManaged(model, configuredRootPackage)

  // FIXME #382 add reference to the runtime lib here
  override def suggestedDependencies: Seq[Artifact] = Seq(
    Artifact(
      BuildInfo.organization,
      // FIXME determine scala version properly
      "akkaserverless-scala-sdk" + "_2.13",
      BuildInfo.version))
}
