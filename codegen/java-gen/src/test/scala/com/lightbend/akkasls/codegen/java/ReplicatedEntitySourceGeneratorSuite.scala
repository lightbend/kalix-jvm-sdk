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

import com.lightbend.akkasls.codegen.TestData
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedCounter
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedCounterMap
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedData
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedMap
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedMultiMap
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedRegister
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedRegisterMap
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedSet
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedVote
import com.lightbend.akkasls.codegen.ModelBuilder.TypeArgument

class ReplicatedEntitySourceGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.javaStyle

  def testEntityServiceImplementation(
      testName: String,
      replicatedData: ReplicatedData,
      expectedImports: String,
      expectedDataType: String,
      expectedEmptyValue: String = ""): Unit =
    test(s"Generated replicated entity service implementation - $testName") {
      assertNoDiff(
        ReplicatedEntitySourceGenerator.replicatedEntitySource(
          service = testData.simpleEntityService(),
          entity = testData.replicatedEntity(replicatedData),
          packageName = "com.example.service",
          className = "MyService"),
        s"""package com.example.service;
            |
            |$expectedImports
            |
            |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
            |//
            |// As long as this file exists it will not be overwritten: you can maintain it yourself,
            |// or delete it so it is regenerated as needed.
            |
            |/** A replicated entity. */
            |public class MyService extends AbstractMyService {
            |  @SuppressWarnings("unused")
            |  private final String entityId;
            |
            |  public MyService(ReplicatedEntityContext context) {
            |    this.entityId = context.entityId();
            |  }
            |$expectedEmptyValue
            |  @Override
            |  public Effect<Empty> set($expectedDataType currentData, ServiceOuterClass.SetValue setValue) {
            |    return effects().error("The command handler for `Set` is not implemented, yet");
            |  }
            |
            |  @Override
            |  public Effect<ServiceOuterClass.MyState> get($expectedDataType currentData, ServiceOuterClass.GetValue getValue) {
            |    return effects().error("The command handler for `Get` is not implemented, yet");
            |  }
            |}
            |""".stripMargin)
    }

  def domainType(name: String): TypeArgument = TypeArgument(name, testData.domainProto())

  testEntityServiceImplementation(
    "ReplicatedCounter",
    ReplicatedCounter,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedCounter")

  testEntityServiceImplementation(
    "ReplicatedRegister (with protobuf message type)",
    ReplicatedRegister(domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedRegister<EntityOuterClass.SomeValue>",
    """|
       |  @Override
       |  public EntityOuterClass.SomeValue emptyValue() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value");
       |  }
       |""".stripMargin)

  testEntityServiceImplementation(
    "ReplicatedRegister (with protobuf scalar type)",
    ReplicatedRegister(domainType("bytes")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister;
       |import com.external.Empty;
       |import com.google.protobuf.ByteString;
       |""".stripMargin.trim,
    "ReplicatedRegister<ByteString>",
    """|
       |  @Override
       |  public ByteString emptyValue() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value");
       |  }
       |""".stripMargin)

  testEntityServiceImplementation(
    "ReplicatedSet (with protobuf message type)",
    ReplicatedSet(domainType("SomeElement")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedSet<EntityOuterClass.SomeElement>")

  testEntityServiceImplementation(
    "ReplicatedSet (with protobuf scalar type)",
    ReplicatedSet(domainType("string")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedSet<String>")

  testEntityServiceImplementation(
    "ReplicatedMap (with protobuf message type)",
    ReplicatedMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedData;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMap;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedMap<EntityOuterClass.SomeKey, ReplicatedData>")

  testEntityServiceImplementation(
    "ReplicatedMap (with protobuf scalar type)",
    ReplicatedMap(domainType("int32")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedData;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMap;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedMap<Integer, ReplicatedData>")

  testEntityServiceImplementation(
    "ReplicatedCounterMap (with protobuf message type)",
    ReplicatedCounterMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedCounterMap<EntityOuterClass.SomeKey>")

  testEntityServiceImplementation(
    "ReplicatedCounterMap (with protobuf scalar type)",
    ReplicatedCounterMap(domainType("string")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedCounterMap<String>")

  testEntityServiceImplementation(
    "ReplicatedRegisterMap (with protobuf message type)",
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedRegisterMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testEntityServiceImplementation(
    "ReplicatedRegisterMap (with protobuf scalar types)",
    ReplicatedRegisterMap(domainType("double"), domainType("string")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedRegisterMap<Double, String>")

  testEntityServiceImplementation(
    "ReplicatedMultiMap (with protobuf message type)",
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
       |import com.example.service.domain.EntityOuterClass;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedMultiMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testEntityServiceImplementation(
    "ReplicatedMultiMap (with protobuf scalar types)",
    ReplicatedMultiMap(domainType("sint32"), domainType("double")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedMultiMap<Integer, Double>")

  testEntityServiceImplementation(
    "ReplicatedVote",
    ReplicatedVote,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedVote;
       |import com.external.Empty;
       |""".stripMargin.trim,
    "ReplicatedVote")

  def testAbstractEntityService(
      replicatedData: ReplicatedData,
      expectedImports: String,
      expectedBaseClass: String,
      expectedDataType: String): Unit =
    test(s"Generated abstract replicated entity service - ${replicatedData.name}") {
      assertNoDiff(
        EntityServiceSourceGenerator.interfaceSource(
          service = testData.simpleEntityService(),
          entity = testData.replicatedEntity(replicatedData),
          packageName = "com.example.service",
          className = "MyService"),
        s"""|/* This code is managed by Akka Serverless tooling.
            | * It will be re-generated to reflect any changes to your protobuf definitions.
            | * DO NOT EDIT
            | */
            |package com.example.service;
            |
            |$expectedImports
            |import com.external.Empty;
            |
            |/** A replicated entity. */
            |public abstract class AbstractMyService extends $expectedBaseClass {
            |
            |  /** Command handler for "Set". */
            |  public abstract Effect<Empty> set($expectedDataType currentData, ServiceOuterClass.SetValue setValue);
            |
            |  /** Command handler for "Get". */
            |  public abstract Effect<ServiceOuterClass.MyState> get($expectedDataType currentData, ServiceOuterClass.GetValue getValue);
            |
            |}
            |""".stripMargin)
    }

  testAbstractEntityService(
    ReplicatedCounter,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterEntity;
       |""".stripMargin.trim,
    "ReplicatedCounterEntity",
    "ReplicatedCounter")

  testAbstractEntityService(
    ReplicatedRegister(domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegisterEntity<EntityOuterClass.SomeValue>",
    "ReplicatedRegister<EntityOuterClass.SomeValue>")

  testAbstractEntityService(
    ReplicatedSet(domainType("SomeElement")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedSetEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedSetEntity<EntityOuterClass.SomeElement>",
    "ReplicatedSet<EntityOuterClass.SomeElement>")

  testAbstractEntityService(
    ReplicatedMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedData;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMapEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMapEntity<EntityOuterClass.SomeKey, ReplicatedData>",
    "ReplicatedMap<EntityOuterClass.SomeKey, ReplicatedData>")

  testAbstractEntityService(
    ReplicatedCounterMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMapEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedCounterMapEntity<EntityOuterClass.SomeKey>",
    "ReplicatedCounterMap<EntityOuterClass.SomeKey>")

  testAbstractEntityService(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMapEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegisterMapEntity<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>",
    "ReplicatedRegisterMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testAbstractEntityService(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMapEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMultiMapEntity<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>",
    "ReplicatedMultiMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testAbstractEntityService(
    ReplicatedVote,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedVote;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedVoteEntity;
       |""".stripMargin.trim,
    "ReplicatedVoteEntity",
    "ReplicatedVote")

  def testEntityHandler(replicatedData: ReplicatedData, expectedImports: String, expectedDataType: String): Unit =
    test(s"Generated replicated entity handler - ${replicatedData.name}") {
      assertNoDiff(
        ReplicatedEntitySourceGenerator.replicatedEntityHandler(
          service = testData.simpleEntityService(),
          entity = testData.replicatedEntity(replicatedData),
          packageName = "com.example.service",
          className = "MyService"),
        s"""|/* This code is managed by Akka Serverless tooling.
            | * It will be re-generated to reflect any changes to your protobuf definitions.
            | * DO NOT EDIT
            | */
            |package com.example.service;
            |
            |import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler;
            |import com.akkaserverless.javasdk.replicatedentity.CommandContext;
            |$expectedImports
            |import com.external.Empty;
            |
            |/**
            | * A replicated entity handler that is the glue between the Protobuf service <code>MyService</code>
            | * and the command handler methods in the <code>MyReplicatedEntity</code> class.
            | */
            |public class MyServiceHandler extends ReplicatedEntityHandler<$expectedDataType, MyReplicatedEntity> {
            |
            |  public MyServiceHandler(MyReplicatedEntity entity) {
            |    super(entity);
            |  }
            |
            |  @Override
            |  public ReplicatedEntity.Effect<?> handleCommand(
            |      String commandName, $expectedDataType data, Object command, CommandContext context) {
            |    switch (commandName) {
            |
            |      case "Set":
            |        return entity().set(data, (ServiceOuterClass.SetValue) command);
            |
            |      case "Get":
            |        return entity().get(data, (ServiceOuterClass.GetValue) command);
            |
            |      default:
            |        throw new ReplicatedEntityHandler.CommandHandlerNotFound(commandName);
            |    }
            |  }
            |}
            |""".stripMargin)
    }

  testEntityHandler(
    ReplicatedCounter,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |""".stripMargin.trim,
    "ReplicatedCounter")

  testEntityHandler(
    ReplicatedRegister(domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegister<EntityOuterClass.SomeValue>")

  testEntityHandler(
    ReplicatedSet(domainType("SomeElement")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedSet<EntityOuterClass.SomeElement>")

  testEntityHandler(
    ReplicatedMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedData;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMap<EntityOuterClass.SomeKey, ReplicatedData>")

  testEntityHandler(
    ReplicatedCounterMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedCounterMap<EntityOuterClass.SomeKey>")

  testEntityHandler(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegisterMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testEntityHandler(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMultiMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>")

  testEntityHandler(
    ReplicatedVote,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedVote;
       |""".stripMargin.trim,
    "ReplicatedVote")

  def testEntityProvider(
      replicatedData: ReplicatedData,
      expectedImports: String,
      expectedDataType: String,
      expectedDescriptors: String): Unit =
    test(s"Generated replicated entity provider - ${replicatedData.name}") {
      assertNoDiff(
        ReplicatedEntitySourceGenerator.replicatedEntityProvider(
          service = testData.simpleEntityService(),
          entity = testData.replicatedEntity(replicatedData),
          packageName = "com.example.service",
          className = "MyService"),
        s"""|/* This code is managed by Akka Serverless tooling.
            | * It will be re-generated to reflect any changes to your protobuf definitions.
            | * DO NOT EDIT
            | */
            |package com.example.service;
            |
            |$expectedImports
            |import com.external.Empty;
            |import com.external.ExternalDomain;
            |import com.google.protobuf.Descriptors;
            |import java.util.function.Function;
            |
            |/**
            | * A replicated entity provider that defines how to register and create the entity for
            | * the Protobuf service <code>MyService</code>.
            | *
            | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
            | */
            |public class MyServiceProvider implements ReplicatedEntityProvider<$expectedDataType, MyService> {
            |
            |  private final Function<ReplicatedEntityContext, MyService> entityFactory;
            |  private final ReplicatedEntityOptions options;
            |
            |  /** Factory method of MyServiceProvider */
            |  public static MyServiceProvider of(Function<ReplicatedEntityContext, MyService> entityFactory) {
            |    return new MyServiceProvider(entityFactory, ReplicatedEntityOptions.defaults());
            |  }
            |
            |  private MyServiceProvider(
            |      Function<ReplicatedEntityContext, MyService> entityFactory,
            |      ReplicatedEntityOptions options) {
            |    this.entityFactory = entityFactory;
            |    this.options = options;
            |  }
            |
            |  @Override
            |  public final ReplicatedEntityOptions options() {
            |    return options;
            |  }
            |
            |  public final MyServiceProvider withOptions(ReplicatedEntityOptions options) {
            |    return new MyServiceProvider(entityFactory, options);
            |  }
            |
            |  @Override
            |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
            |    return ServiceOuterClass.getDescriptor().findServiceByName("MyService");
            |  }
            |
            |  @Override
            |  public final String entityType() {
            |    return "MyReplicatedEntity";
            |  }
            |
            |  @Override
            |  public final MyServiceHandler newHandler(ReplicatedEntityContext context) {
            |    return new MyServiceHandler(entityFactory.apply(context));
            |  }
            |
            |  @Override
            |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
            |    return new Descriptors.FileDescriptor[] {
            |$expectedDescriptors
            |      ServiceOuterClass.getDescriptor()
            |    };
            |  }
            |}
            |""".stripMargin)
    }

  testEntityProvider(
    ReplicatedCounter,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |""".stripMargin.trim,
    "ReplicatedCounter",
    """|      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedRegister(domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegister;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegister<EntityOuterClass.SomeValue>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedSet(domainType("SomeElement")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedSet<EntityOuterClass.SomeElement>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedData;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMap<EntityOuterClass.SomeKey, ReplicatedData>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedCounterMap(domainType("SomeKey")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedCounterMap<EntityOuterClass.SomeKey>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedRegisterMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedRegisterMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedMultiMap(domainType("SomeKey"), domainType("SomeValue")),
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
       |import com.example.service.domain.EntityOuterClass;
       |""".stripMargin.trim,
    "ReplicatedMultiMap<EntityOuterClass.SomeKey, EntityOuterClass.SomeValue>",
    """|      EntityOuterClass.getDescriptor(),
       |      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

  testEntityProvider(
    ReplicatedVote,
    """|import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
       |import com.akkaserverless.javasdk.replicatedentity.ReplicatedVote;
       |""".stripMargin.trim,
    "ReplicatedVote",
    """|      ExternalDomain.getDescriptor(),
       |""".stripMargin.stripTrailing)

}
