/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.ClassMessageType

object Types {
  object View {
    val UpdateHandlerNotFound = ClassMessageType("kalix.javasdk.impl.view.UpdateHandlerNotFound")
    val ViewRouter = ClassMessageType("kalix.javasdk.impl.view.ViewRouter")
    val ViewMultiTableRouter = ClassMessageType("kalix.javasdk.impl.view.ViewMultiTableRouter")
    val View = ClassMessageType("kalix.javasdk.view.View")
    val ViewProvider = ClassMessageType("kalix.javasdk.view.ViewProvider")
    val ViewCreationContext = ClassMessageType("kalix.javasdk.view.ViewCreationContext")
    val ViewOptions = ClassMessageType("kalix.javasdk.view.ViewOptions")
    val ViewContext = ClassMessageType("kalix.javasdk.view.ViewContext")
  }

  val Descriptors = ClassMessageType("com.google.protobuf.Descriptors")
  val EmptyProto = ClassMessageType("com.google.protobuf.EmptyProto")
  val Function = ClassMessageType("java.util.function.Function")

}
