/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

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

  val knownGoogleProto: PackageNaming =
    PackageNaming(
      "EXT",
      "google.protobuf",
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
          FullyQualifiedName("Empty", knownGoogleProto),
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

  def simpleViewService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.ViewService =
    ModelBuilder.ViewService(
      FullyQualifiedName(s"MyService$suffix", proto),
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
        ),
        ModelBuilder.Command(
          FullyQualifiedName("MyQuery", proto),
          FullyQualifiedName("QueryRequest", proto),
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
      entityType = s"my-eventsourcedentity$suffix-persistence",
      Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto(suffix)))
      )
    )

  def valueEntity(suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      FullyQualifiedName(s"MyValueEntity$suffix", domainProto(suffix)),
      entityType = s"my-valueentity$suffix-persistence",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))
    )
}
