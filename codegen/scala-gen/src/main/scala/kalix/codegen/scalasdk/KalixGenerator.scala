/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import kalix.codegen.scalasdk.impl.SourceGenerator
import protocbridge.Artifact
import kalix.codegen.{ File, ModelBuilder }

object KalixGenerator extends AbstractKalixGenerator {

  override def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] =
    SourceGenerator.generateManaged(model, configuredRootPackage)

  // FIXME #382 add reference to the runtime lib here
  override def suggestedDependencies: Seq[Artifact] = Seq(
    Artifact(BuildInfo.organization, "kalix-scala-sdk-protobuf", BuildInfo.version, crossVersion = true))
}
