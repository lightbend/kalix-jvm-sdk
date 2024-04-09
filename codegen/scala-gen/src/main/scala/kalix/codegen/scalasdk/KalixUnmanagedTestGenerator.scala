/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import kalix.codegen.scalasdk.impl.SourceGenerator
import kalix.codegen.{ File, ModelBuilder }
import protocbridge.Artifact

object KalixUnmanagedTestGenerator extends AbstractKalixGenerator {
  override def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] =
    SourceGenerator.generateUnmanagedTest(model, configuredRootPackage)

  override def suggestedDependencies: Seq[Artifact] = Nil
}
