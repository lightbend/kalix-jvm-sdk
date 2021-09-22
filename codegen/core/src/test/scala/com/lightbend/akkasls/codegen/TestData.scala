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

object TestData {
  val defaultPackageNamingTemplate: PackageNaming =
    PackageNaming(
      "undefined.proto",
      "Undefined",
      "undefined",
      None,
      None,
      Some("UndefinedOuterClass"),
      javaMultipleFiles = false)

  def apply(): TestData =
    apply(defaultPackageNamingTemplate)

  def apply(packageNamingTemplate: PackageNaming): TestData = {
    new TestData(packageNamingTemplate)
  }
}

/**
 * Used by java and scala codegen projects for their tests
 */
class TestData(packageNamingTemplate: PackageNaming) {

  def simple(): ModelBuilder.Model = {
    val service = simpleEntityService()
    val entity = valueEntity()
    ModelBuilder.Model(
      services = Map(service.componentFullName -> service),
      entities = Map(entity.fqn.fullQualifiedName -> entity))
  }

  def serviceProto(suffix: String = ""): PackageNaming =
    packageNamingTemplate.copy(
      "my_service.proto",
      s"MyService$suffix",
      "com.example.service",
      javaOuterClassnameOption = packageNamingTemplate.javaOuterClassnameOption.map(_ => s"ServiceOuterClass$suffix"))

  def domainProto(suffix: String = ""): PackageNaming =
    packageNamingTemplate.copy(
      "domain.proto",
      s"Domain$suffix",
      "com.example.service.domain",
      javaOuterClassnameOption = packageNamingTemplate.javaOuterClassnameOption.map(_ => s"EntityOuterClass$suffix"))

  val externalProto: PackageNaming =
    PackageNaming("external_domain.proto", "ExternalDomain", "com.external", None, None, None, javaMultipleFiles = true)

  val googleProto: PackageNaming =
    PackageNaming(
      "google_proto.proto",
      "GoogleProto",
      "com.google.protobuf",
      None,
      None,
      None,
      javaMultipleFiles = true)

  def command(
      name: String,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName,
      streamedInput: Boolean = false,
      streamedOutput: Boolean = false,
      inFromTopic: Boolean = false,
      outToTopic: Boolean = false): ModelBuilder.Command =
    ModelBuilder.Command(name, inputType, outputType, streamedInput, streamedOutput, inFromTopic, outToTopic)

  def simpleEntityService(proto: PackageNaming = serviceProto(), suffix: String = ""): ModelBuilder.EntityService =
    ModelBuilder.EntityService(
      FullyQualifiedName(s"MyService$suffix", proto),
      List(
        command("Set", FullyQualifiedName("SetValue", proto), FullyQualifiedName("Empty", externalProto)),
        command("Get", FullyQualifiedName("GetValue", proto), FullyQualifiedName("MyState", proto))),
      s"com.example.Entity$suffix")

  def simpleActionService(proto: PackageNaming = serviceProto()): ModelBuilder.ActionService = {

    ModelBuilder.ActionService(
      FullyQualifiedName(proto.name, proto.name, proto),
      List(
        command("SimpleMethod", FullyQualifiedName("MyRequest", proto), FullyQualifiedName("Empty", externalProto)),
        command(
          "StreamedOutputMethod",
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedOutput = true),
        command(
          "StreamedInputMethod",
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = true),
        command(
          "FullStreamedMethod",
          FullyQualifiedName("MyRequest", proto),
          FullyQualifiedName("Empty", externalProto),
          streamedInput = true,
          streamedOutput = true)))
  }

  def simpleJsonPubSubActionService(proto: PackageNaming = serviceProto()): ModelBuilder.ActionService = {
    ModelBuilder.ActionService(
      FullyQualifiedName(proto.name, proto.name, proto),
      List(
        command(
          "InFromTopic",
          FullyQualifiedName("Any", googleProto),
          FullyQualifiedName("Empty", googleProto),
          inFromTopic = true),
        command(
          "OutToTopic",
          FullyQualifiedName("EntityUpdated", domainProto()),
          FullyQualifiedName("Any", googleProto),
          outToTopic = true)))
  }

  def simpleViewService(proto: PackageNaming = serviceProto(), suffix: String = ""): ModelBuilder.ViewService = {
    val updates = List(
      command(
        "Created",
        FullyQualifiedName("EntityCreated", domainProto(suffix)),
        FullyQualifiedName("ViewState", proto)),
      command(
        "Updated",
        FullyQualifiedName("EntityUpdated", domainProto(suffix)),
        FullyQualifiedName("ViewState", proto)))
    ModelBuilder.ViewService(
      FullyQualifiedName(s"MyService${suffix}", s"MyService${suffix}View", proto),
      List(
        command(
          "Created",
          FullyQualifiedName("EntityCreated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto)),
        command(
          "Updated",
          FullyQualifiedName("EntityUpdated", domainProto(suffix)),
          FullyQualifiedName("ViewState", proto))),
      s"MyService$suffix",
      updates,
      updates)
  }

  def eventSourcedEntity(suffix: String = ""): ModelBuilder.EventSourcedEntity =
    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName(s"MyEntity$suffix", domainProto(suffix)),
      s"MyEntity$suffix",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix))),
      List(ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto(suffix)))))

  def valueEntity(): ModelBuilder.ValueEntity = valueEntity("")
  def valueEntity(suffix: String): ModelBuilder.ValueEntity =
    valueEntity(domainProto(suffix), suffix)
  def valueEntity(parent: PackageNaming, suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      parent.pkg + s"MyValueEntity$suffix",
      FullyQualifiedName(s"MyValueEntity$suffix", parent),
      s"MyValueEntity$suffix",
      ModelBuilder.State(FullyQualifiedName("MyState", parent)))

  def replicatedEntity(data: ModelBuilder.ReplicatedData, suffix: String = ""): ModelBuilder.ReplicatedEntity =
    ModelBuilder.ReplicatedEntity(
      FullyQualifiedName(s"MyReplicatedEntity$suffix", domainProto(suffix)),
      s"MyReplicatedEntity$suffix",
      data)
}
