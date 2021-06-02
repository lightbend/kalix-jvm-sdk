/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Paths }

class ViewServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleViewService(protoRef)
    val entity   = TestData.eventSourcedEntity()

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      ViewServiceSourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { View } from "@lightbend/akkaserverless-javascript-sdk";
        |
        |/**
        | * Type definitions.
        | * These types have been generated based on your proto source.
        | * A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.
        | * 
        | * MyService; a strongly typed extension of View derived from your proto source
        | * @typedef { import("../../lib/generated/myservice").MyService } MyService
        | */
        |
        |/**
        | * @type MyService
        | */
        |const view = new View(
        |  [
        |    "myentity1.proto",
        |    "someother.proto"
        |  ],
        |  "com.example.service.MyService",
        |  {
        |    includeDirs: ["./src/proto"],
        |    serializeFallbackToJson: true,
        |    viewId: "my-view-id"
        |  }
        |);
        |
        |view.setUpdateHandlers({
        |  Created(event, state, ctx) {
        |    return ctx.fail("The update handler for `Created` is not implemented, yet");
        |  },
        |  Updated(event, state, ctx) {
        |    return ctx.fail("The update handler for `Updated` is not implemented, yet");
        |  }
        |});
        |
        |export default view;""".stripMargin
    )
  }

  test("typedef source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleViewService(protoRef)
    val entity   = TestData.eventSourcedEntity()

    val sourceDoc =
      ViewServiceSourceGenerator.typedefSource(
        service,
        entity
      )
    // assertEquals(
    //   sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
    //   """import {
    //     |  TypedEventSourcedEntity,
    //     |  EventSourcedCommandContext
    //     |} from "../akkaserverless";
    //     |import proto from "./proto";
    //     |
    //     |export type State = proto.com.example.service.persistence.IMyState;
    //     |export type Event = proto.com.example.service.persistence.ISetEvent;
    //     |export type Command =
    //     |  | proto.com.example.service.ISetValue
    //     |  | proto.com.example.service.IGetValue;
    //     |
    //     |export type EventHandlers = {
    //     |  SetEvent: (
    //     |    event: proto.com.example.service.persistence.ISetEvent,
    //     |    state: State
    //     |  ) => State;
    //     |};
    //     |
    //     |export type CommandHandlers = {
    //     |  Set: (
    //     |    command: proto.com.example.service.ISetValue,
    //     |    state: State,
    //     |    ctx: EventSourcedCommandContext<Event>
    //     |  ) => void;
    //     |  Get: (
    //     |    command: proto.com.example.service.IGetValue,
    //     |    state: State,
    //     |    ctx: EventSourcedCommandContext<Event>
    //     |  ) => proto.com.example.service.IMyState;
    //     |};
    //     |
    //     |export type MyService = TypedEventSourcedEntity<
    //     |  State,
    //     |  EventHandlers,
    //     |  CommandHandlers
    //     |>;
    //     |""".stripMargin
    // )
  }
}
