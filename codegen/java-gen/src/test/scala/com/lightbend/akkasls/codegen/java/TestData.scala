/*
 * Copyright (c) Lightbend Inc. 2021
 *
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
      Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))),
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
