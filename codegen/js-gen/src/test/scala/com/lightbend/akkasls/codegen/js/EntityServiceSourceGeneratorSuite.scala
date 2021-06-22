/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Paths }

class EntityServiceSourceGeneratorSuite extends munit.FunSuite {

  test("EventSourcedEntity source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef)
    val entity   = TestData.eventSourcedEntity()

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      EntityServiceSourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import akkaserverless from "@lightbend/akkaserverless-javascript-sdk";
        |const EventSourcedEntity = akkaserverless.EventSourcedEntity;
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef)
    val entity   = TestData.valueEntity()

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      EntityServiceSourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service,
        entity
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import akkaserverless from "@lightbend/akkaserverless-javascript-sdk";
        |const ValueEntity = akkaserverless.ValueEntity;
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef)
    val entity   = TestData.eventSourcedEntity()

    val sourceDoc =
      EntityServiceSourceGenerator.typedefSource(
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef)
    val entity   = TestData.valueEntity()

    val sourceDoc =
      EntityServiceSourceGenerator.typedefSource(
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef, "1")
    val entity   = TestData.eventSourcedEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      EntityServiceSourceGenerator.testSource(service, entity, testSourceDirectory, sourceDirectory)
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef, "1")
    val entity   = TestData.valueEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      EntityServiceSourceGenerator.testSource(service, entity, testSourceDirectory, sourceDirectory)
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
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleEntityService(protoRef, "1")
    val entity   = TestData.valueEntity()

    val testSourceDirectory = Paths.get("./test/js")
    val sourceDirectory     = Paths.get("./src/js")
    val sourceDoc =
      EntityServiceSourceGenerator.integrationTestSource(
        service,
        entity,
        testSourceDirectory,
        sourceDirectory
      )
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

}
