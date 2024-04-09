/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import protocbridge.SandboxedJvmGenerator

object genUnmanagedTest {
  def apply(options: Seq[String] = Seq.empty): (SandboxedJvmGenerator, Seq[String]) =
    gen(options, "kalix.codegen.scalasdk.KalixUnmanagedTestGenerator$")
}
