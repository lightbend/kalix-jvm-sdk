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

package com.akkaserverless.codegen.scalasdk

import com.akkaserverless.codegen.scalasdk.impl.ReplicatedEntitySourceGenerator
import com.lightbend.akkasls.codegen.ModelBuilder.{
  ReplicatedCounter,
  ReplicatedCounterMap,
  ReplicatedData,
  ReplicatedMap,
  ReplicatedMultiMap,
  ReplicatedRegister,
  ReplicatedRegisterMap,
  ReplicatedSet,
  ReplicatedVote,
  TypeArgument
}
import com.lightbend.akkasls.codegen.TestData

class ReplicatedEntitySourceGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle

  def domainType(name: String): TypeArgument = TypeArgument(
    name,
    testData.domainProto(),
    TestData.guessDescriptor(testData.domainProto().name, testData.domainProto()))

  def testEntityServiceImplementation(
      testName: String,
      replicatedData: ReplicatedData,
      specificImports: Set[String],
      expectedDataType: String,
      expectedEmptyValue: String = ""): Unit =
    test(s"Generated replicated entity service implementation - $testName") {

      val fixedImports =
        Set(
          "import com.example.service",
          "import com.external.Empty",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext")

      val expectedImports = (fixedImports ++ specificImports).toList.sorted.mkString("\n")

      assertNoDiff(
        ReplicatedEntitySourceGenerator
          .generateImplementationSkeleton(
            service = testData.simpleEntityService(),
            entity = testData.replicatedEntity(replicatedData))
          .content,
        s"""package com.example.service.domain
           |
           |$expectedImports
           |
           |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
           |//
           |// As long as this file exists it will not be overwritten: you can maintain it yourself,
           |// or delete it so it is regenerated as needed.
           |
           |/** A replicated entity. */
           |class MyReplicatedEntity(context: ReplicatedEntityContext) extends AbstractMyReplicatedEntity {
           |
           |$expectedEmptyValue
           |  /** Command handler for "Set". */
           |  def set(currentData: $expectedDataType, setValue: service.SetValue): ReplicatedEntity.Effect[Empty] =
           |    effects.error("The command handler for `Set` is not implemented, yet")
           |
           |  /** Command handler for "Get". */
           |  def get(currentData: $expectedDataType, getValue: service.GetValue): ReplicatedEntity.Effect[service.MyState] =
           |    effects.error("The command handler for `Get` is not implemented, yet")
           |
           |}
           |""".stripMargin)
    }

  testEntityServiceImplementation(
    "ReplicatedCounter",
    ReplicatedCounter,
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter"),
    "ReplicatedCounter")

  testEntityServiceImplementation(
    "ReplicatedRegister (with protobuf message type)",
    ReplicatedRegister(domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister"),
    "ReplicatedRegister[SomeValue]",
    """|  override def emptyValue: SomeValue =
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value")
       |
       |""".stripMargin)

  testEntityServiceImplementation(
    "ReplicatedRegister (with protobuf scalar type)",
    ReplicatedRegister(domainType("bytes")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister",
      "import com.google.protobuf.ByteString"),
    "ReplicatedRegister[ByteString]",
    """|  override def emptyValue: ByteString =
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value")
       |
       |""".stripMargin)

  testEntityServiceImplementation(
    "ReplicatedSet (with protobuf message type)",
    ReplicatedSet(domainType("SomeElement")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet"),
    "ReplicatedSet[SomeElement]")

  testEntityServiceImplementation(
    "ReplicatedSet (with protobuf scalar type)",
    ReplicatedSet(domainType("string")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet"),
    "ReplicatedSet[String]")

  testEntityServiceImplementation(
    "ReplicatedMap (with protobuf message type)",
    ReplicatedMap(domainType("SomeKey")),
    Set(
      "import com.akkaserverless.replicatedentity.ReplicatedData",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap"),
    "ReplicatedMap[SomeKey, ReplicatedData]")

  testEntityServiceImplementation(
    "ReplicatedMap (with protobuf scalar type)",
    ReplicatedMap(domainType("int32")),
    Set(
      "import com.akkaserverless.replicatedentity.ReplicatedData",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap"),
    "ReplicatedMap[Int, ReplicatedData]")

  testEntityServiceImplementation(
    "ReplicatedCounterMap (with protobuf message type)",
    ReplicatedCounterMap(domainType("SomeKey")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap"),
    "ReplicatedCounterMap[SomeKey]")

  testEntityServiceImplementation(
    "ReplicatedCounterMap (with protobuf scalar type)",
    ReplicatedCounterMap(domainType("string")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap"),
    "ReplicatedCounterMap[String]")

  testEntityServiceImplementation(
    "ReplicatedRegisterMap (with protobuf message type)",
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap"),
    "ReplicatedRegisterMap[SomeKey, SomeValue]")

  testEntityServiceImplementation(
    "ReplicatedRegisterMap (with protobuf scalar types)",
    ReplicatedRegisterMap(domainType("double"), domainType("string")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap"),
    "ReplicatedRegisterMap[Double, String]")

  testEntityServiceImplementation(
    "ReplicatedMultiMap (with protobuf message type)",
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap"),
    "ReplicatedMultiMap[SomeKey, SomeValue]")

  testEntityServiceImplementation(
    "ReplicatedMultiMap (with protobuf scalar types)",
    ReplicatedMultiMap(domainType("sint32"), domainType("double")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap"),
    "ReplicatedMultiMap[Int, Double]")

  testEntityServiceImplementation(
    "ReplicatedVote",
    ReplicatedVote,
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote"),
    "ReplicatedVote")

  def testAbstractEntityService(
      replicatedData: ReplicatedData,
      specificImports: Set[String],
      expectedBaseClass: String,
      expectedDataType: String): Unit =
    test(s"Generated abstract replicated entity service - ${replicatedData.name}") {

      val fixedImports =
        Set(
          "import com.example.service",
          "import com.external.Empty",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity")

      val expectedImports = (fixedImports ++ specificImports).toList.sorted.mkString("\n")

      assertNoDiff(
        ReplicatedEntitySourceGenerator
          .abstractEntity(service = testData.simpleEntityService(), entity = testData.replicatedEntity(replicatedData))
          .content,
        s"""package com.example.service.domain
           |
           |$expectedImports
           |
           |// This code is managed by Akka Serverless tooling.
           |// It will be re-generated to reflect any changes to your protobuf definitions.
           |// DO NOT EDIT
           |
           |/** A replicated entity. */
           |abstract class AbstractMyReplicatedEntity extends $expectedBaseClass {
           |
           |  /** Command handler for "Set". */
           |  def set(currentData: $expectedDataType, setValue: service.SetValue): ReplicatedEntity.Effect[Empty]
           |
           |  /** Command handler for "Get". */
           |  def get(currentData: $expectedDataType, getValue: service.GetValue): ReplicatedEntity.Effect[service.MyState]
           |
           |}
           |""".stripMargin)
    }

  testAbstractEntityService(
    ReplicatedCounter,
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterEntity"),
    "ReplicatedCounterEntity",
    "ReplicatedCounter")

  testAbstractEntityService(
    ReplicatedRegister(domainType("SomeValue")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterEntity"),
    "ReplicatedRegisterEntity[SomeValue]",
    "ReplicatedRegister[SomeValue]")

  testAbstractEntityService(
    ReplicatedSet(domainType("SomeElement")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSetEntity"),
    "ReplicatedSetEntity[SomeElement]",
    "ReplicatedSet[SomeElement]")

  testAbstractEntityService(
    ReplicatedMap(domainType("SomeKey")),
    Set(
      "import com.akkaserverless.replicatedentity.ReplicatedData",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMapEntity"),
    "ReplicatedMapEntity[SomeKey, ReplicatedData]",
    "ReplicatedMap[SomeKey, ReplicatedData]")

  testAbstractEntityService(
    ReplicatedCounterMap(domainType("SomeKey")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMapEntity"),
    "ReplicatedCounterMapEntity[SomeKey]",
    "ReplicatedCounterMap[SomeKey]")

  testAbstractEntityService(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMapEntity"),
    "ReplicatedRegisterMapEntity[SomeKey, SomeValue]",
    "ReplicatedRegisterMap[SomeKey, SomeValue]")

  testAbstractEntityService(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMapEntity"),
    "ReplicatedMultiMapEntity[SomeKey, SomeValue]",
    "ReplicatedMultiMap[SomeKey, SomeValue]")

  testAbstractEntityService(
    ReplicatedVote,
    Set(
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVoteEntity"),
    "ReplicatedVoteEntity",
    "ReplicatedVote")

  def testEntityHandler(replicatedData: ReplicatedData, specificImport: String, expectedDataType: String): Unit =
    test(s"Generated replicated entity handler - ${replicatedData.name}") {

      val fixedImports =
        Set(
          "import com.example.service",
          "import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler.CommandHandlerNotFound",
          "import com.akkaserverless.replicatedentity.ReplicatedData",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
          "import com.akkaserverless.scalasdk.impl.replicatedentity.ReplicatedEntityHandler",
          "import com.akkaserverless.scalasdk.replicatedentity.CommandContext")

      val expectedImports = (fixedImports + specificImport).toList.sorted.mkString("\n")

      assertNoDiff(
        ReplicatedEntitySourceGenerator
          .handler(service = testData.simpleEntityService(), entity = testData.replicatedEntity(replicatedData))
          .content,
        s"""package com.example.service.domain
           |
           |$expectedImports
           |
           |// This code is managed by Akka Serverless tooling.
           |// It will be re-generated to reflect any changes to your protobuf definitions.
           |// DO NOT EDIT
           |
           |/**
           | * A replicated entity handler that is the glue between the Protobuf service `MyService`
           | * and the command handler methods in the `MyReplicatedEntity` class.
           | */
           |class MyReplicatedEntityHandler(entity: MyReplicatedEntity)
           |  extends ReplicatedEntityHandler[$expectedDataType, MyReplicatedEntity](entity) {
           |
           |  override def handleCommand(
           |      commandName: String,
           |      data: ReplicatedData,
           |      command: Any,
           |      context: CommandContext): ReplicatedEntity.Effect[_] = {
           |
           |    val scalaData = data.asInstanceOf[$expectedDataType]
           |    commandName match {
           |      case "Set" =>
           |        entity.set(scalaData, command.asInstanceOf[service.SetValue])
           |
           |      case "Get" =>
           |        entity.get(scalaData, command.asInstanceOf[service.GetValue])
           |
           |      case _ =>
           |        throw new CommandHandlerNotFound(commandName)
           |    }
           |  }
           |}
           |""".stripMargin)
    }

  testEntityHandler(
    ReplicatedCounter,
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter",
    "ReplicatedCounter")

  testEntityHandler(
    ReplicatedRegister(domainType("SomeValue")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister",
    "ReplicatedRegister[SomeValue]")

  testEntityHandler(
    ReplicatedSet(domainType("SomeElement")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet",
    "ReplicatedSet[SomeElement]")

  testEntityHandler(
    ReplicatedMap(domainType("SomeKey")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap",
    "ReplicatedMap[SomeKey, ReplicatedData]")

  testEntityHandler(
    ReplicatedCounterMap(domainType("SomeKey")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap",
    "ReplicatedCounterMap[SomeKey]")

  testEntityHandler(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap",
    "ReplicatedRegisterMap[SomeKey, SomeValue]")

  testEntityHandler(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap",
    "ReplicatedMultiMap[SomeKey, SomeValue]")

  testEntityHandler(
    ReplicatedVote,
    "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote",
    "ReplicatedVote")

  def testEntityProvider(
      replicatedData: ReplicatedData,
      specificImports: Set[String],
      expectedDataType: String,
      expectedDescriptors: String): Unit =
    test(s"Generated replicated entity provider - ${replicatedData.name}") {

      val fixedImports =
        Set(
          "import com.example.service",
          "import com.external.ExternalDomainProto",
          "import com.google.protobuf.Descriptors",
          "import scala.collection.immutable",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions",
          "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider")

      val expectedImports = (fixedImports ++ specificImports).toList.sorted.mkString("\n")

      assertNoDiff(
        ReplicatedEntitySourceGenerator
          .provider(service = testData.simpleEntityService(), entity = testData.replicatedEntity(replicatedData))
          .content,
        s"""package com.example.service.domain
           |
           |$expectedImports
           |
           |// This code is managed by Akka Serverless tooling.
           |// It will be re-generated to reflect any changes to your protobuf definitions.
           |// DO NOT EDIT
           |
           |/**
           | * A replicated entity provider that defines how to register and create the entity for
           | * the Protobuf service `MyService`.
           | *
           | * Should be used with the `register` method in [[com.akkaserverless.scalasdk.AkkaServerless]].
           | */
           |object MyReplicatedEntityProvider {
           |  def apply(entityFactory: ReplicatedEntityContext => MyReplicatedEntity): MyReplicatedEntityProvider =
           |    new MyReplicatedEntityProvider(entityFactory, ReplicatedEntityOptions.defaults)
           |
           |  def apply(entityFactory: ReplicatedEntityContext => MyReplicatedEntity, options: ReplicatedEntityOptions): MyReplicatedEntityProvider =
           |    new MyReplicatedEntityProvider(entityFactory, options)
           |}
           |
           |
           |class MyReplicatedEntityProvider private (
           |    entityFactory: ReplicatedEntityContext => MyReplicatedEntity,
           |    override val options: ReplicatedEntityOptions)
           |    extends ReplicatedEntityProvider[$expectedDataType, MyReplicatedEntity] {
           |
           |  override def entityType: String = "MyReplicatedEntity"
           |
           |  override def newHandler(context: ReplicatedEntityContext): MyReplicatedEntityHandler =
           |    new MyReplicatedEntityHandler(entityFactory(context))
           |
           |  override def serviceDescriptor: Descriptors.ServiceDescriptor =
           |    service.MyServiceProto.javaDescriptor.findServiceByName("MyService")
           |
           |  override def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
           |    $expectedDescriptors :: Nil
           |}
           |""".stripMargin)
    }

  testEntityProvider(
    ReplicatedCounter,
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter"),
    "ReplicatedCounter",
    "ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedRegister(domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister"),
    "ReplicatedRegister[SomeValue]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedSet(domainType("SomeElement")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet"),
    "ReplicatedSet[SomeElement]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedMap(domainType("SomeKey")),
    Set(
      "import com.akkaserverless.replicatedentity.ReplicatedData",
      "import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap"),
    "ReplicatedMap[SomeKey, ReplicatedData]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedCounterMap(domainType("SomeKey")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap"),
    "ReplicatedCounterMap[SomeKey]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap"),
    "ReplicatedRegisterMap[SomeKey, SomeValue]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap"),
    "ReplicatedMultiMap[SomeKey, SomeValue]",
    "DomainProto.javaDescriptor :: ExternalDomainProto.javaDescriptor")

  testEntityProvider(
    ReplicatedVote,
    Set("import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote"),
    "ReplicatedVote",
    "ExternalDomainProto.javaDescriptor")
}
