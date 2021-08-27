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

package com.lightbend.akkasls.codegen
package java

object TestData {
  def serviceProto(suffix: String = ""): PackageNaming =
    PackageNaming(
      s"MyService$suffix",
      "com.example.service",
      None,
      None,
      Some(s"ServiceOuterClass$suffix"),
      javaMultipleFiles = false
    )

  def domainProto(suffix: String = ""): PackageNaming =
    PackageNaming(
      s"Domain$suffix",
      "com.example.service.persistence",
      None,
      None,
      Some(s"EntityOuterClass$suffix"),
      javaMultipleFiles = false
    )

  val externalProto: PackageNaming =
    PackageNaming(
      "ExternalDomain",
      "com.external",
      None,
      None,
      None,
      javaMultipleFiles = true
    )

  def simpleEntityService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.EntityService =
    ModelBuilder.EntityService(
      FullyQualifiedName(s"MyService$suffix", proto),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", proto),
          FullyQualifiedName("SetValue", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", proto),
          FullyQualifiedName("GetValue", proto),
          FullyQualifiedName("MyState", proto),
          streamedInput = false,
          streamedOutput = false
        )
      ),
      s"com.example.Entity$suffix"
    )

  def simpleActionService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.ActionService =
    ModelBuilder.ActionService(
      FullyQualifiedName(s"MyService$suffix", proto),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("SimpleMethod", proto),
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("StreamedOutputMethod", proto),
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = false,
          streamedOutput = true
        ),
        ModelBuilder.Command(
          FullyQualifiedName("FullStreamedMethod", proto),
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = true,
          streamedOutput = true
        )
      )
    )

  def simpleViewService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.ViewService =
    ModelBuilder.ViewService(
      FullyQualifiedName(s"MyService${suffix}", s"MyService${suffix}View", proto),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Created", proto),
          FullyQualifiedName("EntityCreated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Updated", proto),
          FullyQualifiedName("EntityUpdated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto),
          streamedInput = false,
          streamedOutput = false
        )
      ),
      s"my-view-id$suffix",
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Created", proto),
          FullyQualifiedName("EntityCreated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto),
          streamedInput = false,
          streamedOutput = false
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Updated", proto),
          FullyQualifiedName("EntityUpdated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto),
          streamedInput = false,
          streamedOutput = false
        )
      )
    )

  def eventSourcedEntity(
      suffix: String = ""
  ): ModelBuilder.EventSourcedEntity =
    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName(s"MyEntity$suffix", domainProto(suffix)),
      s"MyEntity$suffix",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix))),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto(suffix)))
      )
    )

  def valueEntity(suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      FullyQualifiedName(s"MyValueEntity$suffix", domainProto(suffix)),
      s"MyValueEntity$suffix",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))
    )
}
