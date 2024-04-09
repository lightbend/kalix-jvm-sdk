/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import protocbridge.{ Artifact, SandboxedJvmGenerator }

object gen {
  def apply(
      options: Seq[String] = Seq.empty,
      generatorClass: String = "kalix.codegen.scalasdk.KalixGenerator$"): (SandboxedJvmGenerator, Seq[String]) =
    (
      SandboxedJvmGenerator.forModule(
        "scala",
        Artifact(
          kalix.codegen.scalasdk.BuildInfo.organization,
          "kalix-codegen-scala_2.12",
          kalix.codegen.scalasdk.BuildInfo.version),
        generatorClass,
        KalixGenerator.suggestedDependencies),
      options)

}
