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

import kalix.codegen.scalasdk.impl.SourceGenerator
import kalix.codegen.{ File, ModelBuilder }
import protocbridge.Artifact

object KalixUnmanagedGenerator extends AbstractKalixGenerator {
  override def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] =
    SourceGenerator.generateUnmanaged(model, configuredRootPackage)

  override def suggestedDependencies: Seq[Artifact] = Nil
}
