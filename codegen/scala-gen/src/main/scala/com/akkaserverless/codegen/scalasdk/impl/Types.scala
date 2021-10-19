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

package com.akkaserverless.codegen.scalasdk.impl

import com.lightbend.akkasls.codegen.FullyQualifiedName

import scala.reflect.{ classTag, ClassTag }

object Types {
  val EventSourcedEntity =
    FullyQualifiedName.noDescriptor("EventSourcedEntity", "com.akkaserverless.scalasdk.eventsourcedentity")

  val EventSourcedEntityProvider =
    FullyQualifiedName.noDescriptor("EventSourcedEntityProvider", "com.akkaserverless.scalasdk.eventsourcedentity")

  val EventSourcedEntityOptions =
    FullyQualifiedName.noDescriptor("EventSourcedEntityOptions", "com.akkaserverless.scalasdk.eventsourcedentity")

  val EventSourcedEntityContext =
    FullyQualifiedName.noDescriptor("EventSourcedEntityContext", "com.akkaserverless.scalasdk.eventsourcedentity")

  val CommandContext =
    FullyQualifiedName.noDescriptor("CommandContext", "com.akkaserverless.scalasdk.eventsourcedentity")

  val EventSourcedEntityRouter =
    FullyQualifiedName.noDescriptor("EventSourcedEntityRouter", "com.akkaserverless.scalasdk.impl.eventsourcedentity")

  val CommandHandlerNotFound =
    FullyQualifiedName.noDescriptor(
      "CommandHandlerNotFound",
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter")
  val EventHandlerNotFound =
    FullyQualifiedName.noDescriptor(
      "EventHandlerNotFound",
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter")

  def ref[T: ClassTag]: FullyQualifiedName = {
    val ct = classTag[T]
    FullyQualifiedName.noDescriptor(ct.runtimeClass.getSimpleName, ct.runtimeClass.getPackageName)
  }
}
