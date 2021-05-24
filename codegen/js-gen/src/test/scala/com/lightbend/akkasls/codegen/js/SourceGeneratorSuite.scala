/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Files, Paths }
import org.apache.commons.io.FileUtils

class SourceGeneratorSuite extends munit.FunSuite {
  def serviceProto(suffix: String = ""): PackageNaming =
    PackageNaming(
      s"MyService$suffix",
      "com.example.service",
      None,
      None,
      Some(s"OuterClass$suffix"),
      javaMultipleFiles = false
    )

  val domainProto: PackageNaming =
    PackageNaming(
      "Domain",
      "com.example.service.persistence",
      None,
      None,
      None,
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

  def simpleService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.Service =
    ModelBuilder.Service(
      FullyQualifiedName(s"MyService$suffix", proto),
      s"com.example.Entity$suffix",
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", proto),
          FullyQualifiedName("SetValue", proto),
          FullyQualifiedName("Empty", knownGoogleProto)
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", proto),
          FullyQualifiedName("GetValue", proto),
          FullyQualifiedName("MyState", proto)
        )
      )
    )

  def eventSourcedEntity(suffix: String = ""): ModelBuilder.EventSourcedEntity =
    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName(s"MyEntity$suffix", domainProto),
      entityType = s"my-eventsourcedentity$suffix-persistence",
      Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto))),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto))
      )
    )

  def valueEntity(suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      FullyQualifiedName(s"MyValueEntity$suffix", domainProto),
      entityType = s"my-valueentity$suffix-persistence",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto))
    )

  test("generate") {
    val protoSourceDirectory = Files.createTempDirectory("proto-source-generator-test")
    try {
      val sourceDirectory = Files.createTempDirectory("source-generator-test")
      try {
        val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")
        try {
          val integrationTestSourceDirectory =
            Files.createTempDirectory("integration-test-source-generator-test")
          try {
            val generatedSourceDirectory =
              Files.createTempDirectory("generated-source-generator-test")

            try {
              val protoSource1     = protoSourceDirectory.resolve("myservice1.proto")
              val protoSourceFile1 = protoSource1.toFile
              FileUtils.forceMkdir(protoSourceFile1.getParentFile)
              FileUtils.touch(protoSourceFile1)

              val source1     = sourceDirectory.resolve("myentity1.js")
              val sourceFile1 = source1.toFile
              FileUtils.forceMkdir(sourceFile1.getParentFile)
              FileUtils.touch(sourceFile1)
              val typedefSource1     = generatedSourceDirectory.resolve("myentity1.d.ts")
              val typedefSourceFile1 = typedefSource1.toFile
              FileUtils.forceMkdir(typedefSourceFile1.getParentFile)
              FileUtils.touch(typedefSourceFile1)

              val testSource2     = testSourceDirectory.resolve("myvalueentity2.test.js")
              val testSourceFile2 = testSource2.toFile
              FileUtils.forceMkdir(testSourceFile2.getParentFile)
              FileUtils.touch(testSourceFile2)

              val integrationTestSource3 =
                integrationTestSourceDirectory.resolve("myentity3.test.js")
              val integrationTestSourceFile3 = integrationTestSource3.toFile
              FileUtils.forceMkdir(integrationTestSourceFile3.getParentFile)
              FileUtils.touch(integrationTestSourceFile3)

              val protoRef = serviceProto()

              val services = Map(
                "com.example.Service1" -> simpleService(protoRef, "1"),
                "com.example.Service2" -> simpleService(protoRef, "2"),
                "com.example.Service3" -> simpleService(protoRef, "3")
              )

              val entities = Map(
                "com.example.Entity1" -> eventSourcedEntity("1"),
                "com.example.Entity2" -> valueEntity("2"),
                "com.example.Entity3" -> eventSourcedEntity("3")
              )

              val sources = SourceGenerator.generate(
                sourceDirectory.resolve("some.desc"),
                ModelBuilder.Model(services, entities),
                protoSourceDirectory,
                sourceDirectory,
                testSourceDirectory,
                generatedSourceDirectory,
                Some(integrationTestSourceDirectory),
                "index.js"
              )

              assertEquals(Files.size(source1), 0L)
              assertEquals(Files.size(testSource2), 0L)

              assertEquals(
                sources,
                List(
                  generatedSourceDirectory.resolve("myentity1.d.ts"),
                  testSourceDirectory.resolve("myentity1.test.js"),
                  integrationTestSourceDirectory.resolve("myentity1.test.js"),
                  sourceDirectory.resolve("myvalueentity2.js"),
                  generatedSourceDirectory.resolve("myvalueentity2.d.ts"),
                  integrationTestSourceDirectory.resolve("myvalueentity2.test.js"),
                  sourceDirectory.resolve("myentity3.js"),
                  generatedSourceDirectory.resolve("myentity3.d.ts"),
                  testSourceDirectory.resolve("myentity3.test.js"),
                  sourceDirectory.resolve("index.js"),
                  generatedSourceDirectory.resolve("index.js")
                )
              )

              // Test that all files are being written to (all files should start with at least one import)
              sources.foreach(source =>
                assertEquals(Files.readAllBytes(source).take(7).map(_.toChar).mkString, "import ")
              )
            } finally FileUtils.deleteDirectory(generatedSourceDirectory.toFile)
          } finally FileUtils.deleteDirectory(integrationTestSourceDirectory.toFile)
        } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
      } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(protoSourceDirectory.toFile)
  }

  test("don't generate integration tests if no folder provided") {
    val outDir = Files.createTempDirectory("generator-test")

    try {
      val protoRef = serviceProto()

      val services = Map(
        "com.example.Service1" -> simpleService(protoRef, "1")
      )

      val entities = Map(
        "com.example.Entity1" -> eventSourcedEntity("1")
      )

      val sources = SourceGenerator.generate(
        outDir.resolve("some.desc"),
        ModelBuilder.Model(services, entities),
        outDir,
        outDir,
        outDir.resolve("test"),
        outDir.resolve("gen"),
        None,
        "index.js"
      )

      assertEquals(
        sources,
        List(
          outDir.resolve("myentity1.js"),
          outDir.resolve("gen/myentity1.d.ts"),
          outDir.resolve("test/myentity1.test.js"),
          outDir.resolve("index.js"),
          outDir.resolve("gen/index.js")
        )
      )

    } finally FileUtils.deleteDirectory(outDir.toFile)
  }

  test("EventSourcedEntity source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef)
    val entity   = eventSourcedEntity()

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      SourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { EventSourcedEntity } from "@lightbend/akkaserverless-javascript-sdk";
        |
        |/**
        | * Type definitions.
        | * These types have been generated based on your proto source.
        | * A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.
        | * 
        | * State; the serialisable and persistable state of the entity
        | * @typedef { import("../../lib/generated/myservice").State } State
        | * 
        | * Event; the union of all possible event types
        | * @typedef { import("../../lib/generated/myservice").Event } Event
        | * 
        | * MyService; a strongly typed extension of EventSourcedEntity derived from your proto source
        | * @typedef { import("../../lib/generated/myservice").MyService } MyService
        | */
        |
        |/**
        | * @type MyService
        | */
        |const entity = new EventSourcedEntity(
        |  [
        |    "myentity1.proto",
        |    "someother.proto"
        |  ],
        |  "com.example.service.MyService",
        |  "my-eventsourcedentity-persistence",
        |  {
        |    includeDirs: ["./src/proto"],
        |    serializeFallbackToJson: true
        |  }
        |);
        |
        |entity.setInitial(entityId => ({}));
        |
        |entity.setBehavior(state => ({
        |  commandHandlers: {
        |    Set(command, state, ctx) {
        |      return ctx.fail("The command handler for `Set` is not implemented, yet");
        |    },
        |    Get(command, state, ctx) {
        |      return ctx.fail("The command handler for `Get` is not implemented, yet");
        |    }
        |  },
        |  
        |  eventHandlers: {
        |    SetEvent(event, state) {
        |      return state;
        |    }
        |  }
        |}));
        |
        |export default entity;""".stripMargin
    )
  }

  test("ValueEntity source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef)
    val entity   = valueEntity()

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      SourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { ValueEntity } from "@lightbend/akkaserverless-javascript-sdk";
        |
        |/**
        | * Type definitions.
        | * These types have been generated based on your proto source.
        | * A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.
        | * 
        | * State; the serialisable and persistable state of the entity
        | * @typedef { import("../../lib/generated/myservice").State } State
        | * 
        | * MyService; a strongly typed extension of ValueEntity derived from your proto source
        | * @typedef { import("../../lib/generated/myservice").MyService } MyService
        | */
        |
        |/**
        | * @type MyService
        | */
        |const entity = new ValueEntity(
        |  [
        |    "myentity1.proto",
        |    "someother.proto"
        |  ],
        |  "com.example.service.MyService",
        |  "my-valueentity-persistence",
        |  {
        |    includeDirs: ["./src/proto"],
        |    serializeFallbackToJson: true
        |  }
        |);
        |
        |entity.setInitial(entityId => ({}));
        |
        |entity.setCommandHandlers({
        |  Set(command, state, ctx) {
        |    return ctx.fail("The command handler for `Set` is not implemented, yet");
        |  },
        |  Get(command, state, ctx) {
        |    return ctx.fail("The command handler for `Get` is not implemented, yet");
        |  }
        |});
        |
        |export default entity;""".stripMargin
    )
  }

  test("EventSourcedEntity typedef source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef)
    val entity   = eventSourcedEntity()

    val sourceDoc =
      SourceGenerator.typedefSource(
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import {
        |  TypedEventSourcedEntity,
        |  EventSourcedCommandContext
        |} from "../akkaserverless";
        |import proto from "./proto";
        |
        |export type State = proto.com.example.service.persistence.IMyState;
        |export type Event = proto.com.example.service.persistence.ISetEvent;
        |export type Command =
        |  | proto.com.example.service.ISetValue
        |  | proto.com.example.service.IGetValue;
        |
        |export type EventHandlers = {
        |  SetEvent: (
        |    event: proto.com.example.service.persistence.ISetEvent,
        |    state: State
        |  ) => State;
        |};
        |
        |export type CommandHandlers = {
        |  Set: (
        |    command: proto.com.example.service.ISetValue,
        |    state: State,
        |    ctx: EventSourcedCommandContext<Event>
        |  ) => void;
        |  Get: (
        |    command: proto.com.example.service.IGetValue,
        |    state: State,
        |    ctx: EventSourcedCommandContext<Event>
        |  ) => proto.com.example.service.IMyState;
        |};
        |
        |export type MyService = TypedEventSourcedEntity<
        |  State,
        |  EventHandlers,
        |  CommandHandlers
        |>;
        |""".stripMargin
    )
  }

  test("ValueEntity typedef source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef)
    val entity   = valueEntity()

    val sourceDoc =
      SourceGenerator.typedefSource(
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import {
        |  TypedValueEntity,
        |  ValueEntityCommandContext
        |} from "../akkaserverless";
        |import proto from "./proto";
        |
        |export type State = proto.com.example.service.persistence.IMyState;
        |export type Command =
        |  | proto.com.example.service.ISetValue
        |  | proto.com.example.service.IGetValue;
        |
        |export type CommandHandlers = {
        |  Set: (
        |    command: proto.com.example.service.ISetValue,
        |    state: State,
        |    ctx: ValueEntityCommandContext<State>
        |  ) => void;
        |  Get: (
        |    command: proto.com.example.service.IGetValue,
        |    state: State,
        |    ctx: ValueEntityCommandContext<State>
        |  ) => proto.com.example.service.IMyState;
        |};
        |
        |export type MyService = TypedValueEntity<
        |  State,
        |  CommandHandlers
        |>;
        |""".stripMargin
    )
  }

  test("EventSourcedEntity test source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef, "1")
    val entity   = eventSourcedEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      SourceGenerator.testSource(service, entity, testSourceDirectory, sourceDirectory)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { MockEventSourcedEntity } from "./testkit.js";
        |import { expect } from "chai";
        |import myentity from "../../src/js/myentity.js";
        |
        |describe("MyService1", () => {
        |  const entityId = "entityId";
        |  
        |  describe("Set", () => {
        |    it("should...", () => {
        |      const entity = new MockEventSourcedEntity(myentity, entityId);
        |      // TODO: you may want to set fields in addition to the entity id
        |      // const result = entity.handleCommand("Set", { entityId });
        |      
        |      // expect(result).to.deep.equal({});
        |      // expect(entity.error).to.be.undefined;
        |      // expect(entity.state).to.deep.equal({});
        |      // expect(entity.events).to.deep.equal([]);
        |    });
        |  });
        |  
        |  describe("Get", () => {
        |    it("should...", () => {
        |      const entity = new MockEventSourcedEntity(myentity, entityId);
        |      // TODO: you may want to set fields in addition to the entity id
        |      // const result = entity.handleCommand("Get", { entityId });
        |      
        |      // expect(result).to.deep.equal({});
        |      // expect(entity.error).to.be.undefined;
        |      // expect(entity.state).to.deep.equal({});
        |      // expect(entity.events).to.deep.equal([]);
        |    });
        |  });
        |});""".stripMargin
    )
  }

  test("ValueEntity test source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef, "1")
    val entity   = valueEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      SourceGenerator.testSource(service, entity, testSourceDirectory, sourceDirectory)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { MockValueEntity } from "./testkit.js";
        |import { expect } from "chai";
        |import myvalueentity from "../../src/js/myvalueentity.js";
        |
        |describe("MyService1", () => {
        |  const entityId = "entityId";
        |  
        |  describe("Set", () => {
        |    it("should...", () => {
        |      const entity = new MockValueEntity(myvalueentity, entityId);
        |      // TODO: you may want to set fields in addition to the entity id
        |      // const result = entity.handleCommand("Set", { entityId });
        |      
        |      // expect(result).to.deep.equal({});
        |      // expect(entity.error).to.be.undefined;
        |      // expect(entity.state).to.deep.equal({});
        |    });
        |  });
        |  
        |  describe("Get", () => {
        |    it("should...", () => {
        |      const entity = new MockValueEntity(myvalueentity, entityId);
        |      // TODO: you may want to set fields in addition to the entity id
        |      // const result = entity.handleCommand("Get", { entityId });
        |      
        |      // expect(result).to.deep.equal({});
        |      // expect(entity.error).to.be.undefined;
        |      // expect(entity.state).to.deep.equal({});
        |    });
        |  });
        |});""".stripMargin
    )
  }

  test("ValueEntity integration test source") {
    val protoRef = serviceProto()
    val service  = simpleService(protoRef, "1")
    val entity   = valueEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      SourceGenerator.integrationTestSource(service, entity, testSourceDirectory, sourceDirectory)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import akkaserverless from "@lightbend/akkaserverless-javascript-sdk";
        |import { expect } from "chai";
        |import myvalueentity from "../../src/js/myvalueentity.js";
        |
        |const testkit = new akkaserverless.IntegrationTestkit();
        |testkit.addComponent(myvalueentity);
        |
        |const client = () => testkit.clients.MyService1;
        |
        |describe("MyService1", function() {
        |  this.timeout(60000);
        |  
        |  before(done => testkit.start(done));
        |  after(done => testkit.shutdown(done));
        |  
        |  describe("Set", () => {
        |    it("should...", async () => {
        |      // TODO: populate command payload, and provide assertions to match replies
        |      // const result = await client().set({});
        |    });
        |  });
        |  describe("Get", () => {
        |    it("should...", async () => {
        |      // TODO: populate command payload, and provide assertions to match replies
        |      // const result = await client().get({});
        |    });
        |  });
        |});""".stripMargin
    )
  }

  test("generated component index source") {
    val protoRef = serviceProto()

    val services = List(
      simpleService(protoRef, "1"),
      simpleService(protoRef, "2"),
      simpleService(protoRef, "3")
    )

    val sourceDoc = SourceGenerator.generatedComponentIndex(services)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"),
      """import myservice1 from "./myservice1.js";
        |import myservice2 from "./myservice2.js";
        |import myservice3 from "./myservice3.js";
        |
        |export myservice1;
        |export myservice2;
        |export myservice3;
        |
        |export default [myservice1, myservice2, myservice3];""".stripMargin
    )
  }

  test("index source") {
    val generatedComponentIndexPath = Paths.get("./generated/my-generated-index.js")
    val sourceDirectory             = Paths.get("./src/js")

    val sourceDoc = SourceGenerator.indexSource(sourceDirectory, generatedComponentIndexPath)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"),
      """import generatedComponents from "../../generated/my-generated-index.js";
        |
        |generatedComponents.forEach((component) => {
        |  component.start();
        |});""".stripMargin
    )
  }

}
