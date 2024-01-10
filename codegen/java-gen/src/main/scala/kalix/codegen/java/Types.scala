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
