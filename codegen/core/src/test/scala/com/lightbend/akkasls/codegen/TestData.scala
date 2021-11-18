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
  private val javaStylePackageNamingTemplate: PackageNaming =
    PackageNaming(
      "undefined.proto",
      "Undefined",
      "undefined",
      None,
      Some("UndefinedOuterClass"),
      javaMultipleFiles = false)

  def apply(packageNamingTemplate: PackageNaming): TestData = {
    new TestData(packageNamingTemplate)
  }

  val javaStyle: TestData = apply(javaStylePackageNamingTemplate)
  val scalaStyle: TestData = apply(javaStylePackageNamingTemplate.copy(javaOuterClassnameOption = None))

  def guessDescriptor(protoName: String, proto: PackageNaming): Option[FullyQualifiedName] =
    proto.javaOuterClassnameOption match {
      case Some(outer) =>
        Some(
          FullyQualifiedName(outer, outer, proto.copy(javaOuterClassnameOption = None, javaMultipleFiles = true), None))
      case None =>
        Some(
          FullyQualifiedName(
            protoName + "Proto",
            protoName + "Proto",
            proto.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
            None))
    }

  def fullyQualifiedName(name: String, parent: PackageNaming): FullyQualifiedName = {
    FullyQualifiedName(
      name,
      name,
      parent,
      parent.javaOuterClassnameOption match {
        case Some(outer) =>
          Some(
            FullyQualifiedName(
              outer,
              outer,
              parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
              None))
        case None =>
          def capitalize(s: String, capitalizeNext: Boolean = true): String =
            s.headOption match {
              case None      => ""
              case Some('_') => capitalize(s.tail, true)
              case Some(c) =>
                if (capitalizeNext) c.toUpper + capitalize(s.tail, false)
                else c + capitalize(s.tail, false)
            }
          val protoClassName = capitalize(parent.protoFileName.replaceAll(".proto", "") + "Proto")
          Some(
            FullyQualifiedName(
              protoClassName,
              protoClassName,
              parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
              None))
      })
  }
}

/**
 * Used by java and scala codegen projects for their tests
 */
class TestData(val packageNamingTemplate: PackageNaming) {
  import TestData._

  def simple(): ModelBuilder.Model = {
    val service = simpleEntityService()
    val entity = valueEntity()
    ModelBuilder.Model(
      services = Map(service.componentFullName -> service),
      entities = Map(entity.fqn.fullyQualifiedProtoName -> entity))
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

  val mainPackage: PackageNaming = PackageNaming.noDescriptor("com.example")

  val externalProto: PackageNaming =
    PackageNaming(
      "external_domain.proto",
      "ExternalDomain",
      "com.external",
      None,
      packageNamingTemplate.javaOuterClassnameOption.map(_ => "ExternalDomain"),
      javaMultipleFiles = true)

  val googleProto: PackageNaming =
    PackageNaming("google_proto.proto", "GoogleProto", "com.google.protobuf", None, None, javaMultipleFiles = true)

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
      fullyQualifiedName(s"MyService$suffix", proto),
      List(
        command("Set", fullyQualifiedName("SetValue", proto), fullyQualifiedName("Empty", externalProto)),
        command("Get", fullyQualifiedName("GetValue", proto), fullyQualifiedName("MyState", proto))),
      s"com.example.Entity$suffix")

  def simpleActionService(proto: PackageNaming = serviceProto()): ModelBuilder.ActionService = {
    ModelBuilder.ActionService(
      FullyQualifiedName(proto.name, proto.name, proto, guessDescriptor(proto.name, proto)),
      List(
        command("SimpleMethod", fullyQualifiedName("MyRequest", proto), fullyQualifiedName("Empty", externalProto)),
        command(
          "StreamedOutputMethod",
          fullyQualifiedName("MyRequest", proto),
          fullyQualifiedName("Empty", externalProto),
          streamedOutput = true),
        command(
          "StreamedInputMethod",
          fullyQualifiedName("MyRequest", proto),
          fullyQualifiedName("Empty", externalProto),
          streamedInput = true),
        command(
          "FullStreamedMethod",
          fullyQualifiedName("MyRequest", proto),
          fullyQualifiedName("Empty", externalProto),
          streamedInput = true,
          streamedOutput = true)))
  }

  def simpleJsonPubSubActionService(proto: PackageNaming = serviceProto()): ModelBuilder.ActionService = {
    ModelBuilder.ActionService(
      FullyQualifiedName(proto.name, proto.name, proto, guessDescriptor(proto.name, proto)),
      List(
        command(
          "InFromTopic",
          fullyQualifiedName("Any", googleProto),
          fullyQualifiedName("Empty", googleProto),
          inFromTopic = true),
        command(
          "OutToTopic",
          fullyQualifiedName("EntityUpdated", domainProto()),
          fullyQualifiedName("Any", googleProto),
          outToTopic = true)))
  }

  def simpleViewService(proto: PackageNaming = serviceProto(), suffix: String = ""): ModelBuilder.ViewService = {
    val updates = List(
      command(
        "Created",
        fullyQualifiedName("EntityCreated", domainProto(suffix)),
        fullyQualifiedName("ViewState", proto)),
      command(
        "Updated",
        fullyQualifiedName("EntityUpdated", domainProto(suffix)),
        fullyQualifiedName("ViewState", proto)))
    ModelBuilder.ViewService(
      FullyQualifiedName(
        s"MyService${suffix}",
        s"MyService${suffix}View",
        proto,
        guessDescriptor(s"MyService${suffix}", proto)),
      List(
        command(
          "Created",
          fullyQualifiedName("EntityCreated", domainProto(suffix)),
          fullyQualifiedName("ViewState", proto)),
        command(
          "Updated",
          fullyQualifiedName("EntityUpdated", domainProto(suffix)),
          fullyQualifiedName("ViewState", proto))),
      s"MyService$suffix",
      updates,
      updates,
      List(
        command(
          "Query",
          fullyQualifiedName("QueryRequest", domainProto(suffix)),
          fullyQualifiedName("ViewState", proto))))
  }

  def eventSourcedEntity(suffix: String = ""): ModelBuilder.EventSourcedEntity =
    ModelBuilder.EventSourcedEntity(
      fullyQualifiedName(s"MyEntity$suffix", domainProto(suffix)),
      s"MyEntity$suffix",
      ModelBuilder.State(fullyQualifiedName("MyState", domainProto(suffix))),
      List(ModelBuilder.Event(fullyQualifiedName("SetEvent", domainProto(suffix)))))

  def valueEntity(): ModelBuilder.ValueEntity = valueEntity("")
  def valueEntity(suffix: String): ModelBuilder.ValueEntity =
    valueEntity(domainProto(suffix), suffix)
  def valueEntity(parent: PackageNaming, suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      fullyQualifiedName(s"MyValueEntity$suffix", parent),
      s"MyValueEntity$suffix",
      ModelBuilder.State(fullyQualifiedName("MyState", parent)))

  def replicatedEntity(data: ModelBuilder.ReplicatedData, suffix: String = ""): ModelBuilder.ReplicatedEntity =
    ModelBuilder.ReplicatedEntity(
      fullyQualifiedName(s"MyReplicatedEntity$suffix", domainProto(suffix)),
      s"MyReplicatedEntity$suffix",
      data)
}
