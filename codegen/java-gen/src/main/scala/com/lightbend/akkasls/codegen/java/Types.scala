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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen.FullyQualifiedName

object Types {
  object View {
    val UpdateHandlerNotFound = FQN("com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound")
    val ViewRouter = FQN("com.akkaserverless.javasdk.impl.view.ViewRouter")
    val View = FQN("com.akkaserverless.javasdk.view.View")
    val ViewProvider = FQN("com.akkaserverless.javasdk.view.ViewProvider")
    val ViewCreationContext = FQN("com.akkaserverless.javasdk.view.ViewCreationContext")
    val ViewOptions = FQN("com.akkaserverless.javasdk.view.ViewOptions")
    val ViewContext = FQN("com.akkaserverless.javasdk.view.ViewContext")
  }

  val Descriptors = FQN("com.google.protobuf.Descriptors")
  val EmptyProto = FQN("com.google.protobuf.EmptyProto")
  val Function = FQN("java.util.function.Function")

  private def FQN(fqn: String): FullyQualifiedName =
    FullyQualifiedName.noDescriptor(fqn)
}
