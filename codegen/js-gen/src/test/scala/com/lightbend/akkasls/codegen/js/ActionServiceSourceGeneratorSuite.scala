/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Paths }

class ActionServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleActionService(protoRef)

    val protoSources             = List(Paths.get("myentity1.proto"), Paths.get("someother.proto"))
    val protobufSourceDirectory  = Paths.get("./src/proto")
    val sourceDirectory          = Paths.get("./src/js")
    val generatedSourceDirectory = Paths.get("./lib/generated")

    val sourceDoc =
      ActionServiceSourceGenerator.source(
        protoSources,
        protobufSourceDirectory,
        sourceDirectory,
        generatedSourceDirectory,
        service
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """/* This code was initialised by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |
        |import { Action } from "@lightbend/akkaserverless-javascript-sdk";
        |
        |/**
        | * Type definitions.
        | * These types have been generated based on your proto source.
        | * A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.
        | * 
        | * MyService; a strongly typed extension of Action derived from your proto source
        | * @typedef { import("../../lib/generated/myservice").MyService } MyService
        | */
        |
        |/**
        | * @type MyService
        | */
        |const action = new Action(
        |  [
        |    "myentity1.proto",
        |    "someother.proto"
        |  ],
        |  "com.example.service.MyService",
        |  {
        |    includeDirs: ["./src/proto"],
        |    serializeFallbackToJson: true
        |  }
        |);
        |
        |action.commandHandlers = {
        |  Simple(request, ctx) {
        |    throw new Error("The command handler for `Simple` is not implemented, yet");
        |  },
        |  StreamedIn(ctx) {
        |    throw new Error("The command handler for `StreamedIn` is not implemented, yet");
        |  },
        |  StreamedOut(request, ctx) {
        |    throw new Error("The command handler for `StreamedOut` is not implemented, yet");
        |  },
        |  FullyStreamed(ctx) {
        |    throw new Error("The command handler for `FullyStreamed` is not implemented, yet");
        |  }
        |};
        |
        |export default action;""".stripMargin
    )
  }

  test("typedef source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleActionService(protoRef)

    val sourceDoc =
      ActionServiceSourceGenerator.typedefSource(
        service
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |import {
        |  TypedAction,
        |  ActionCommandContext,
        |  StreamedInCommandContext,
        |  StreamedOutCommandContext
        |} from "../akkaserverless";
        |import proto from "./proto";
        |
        |export type CommandHandlers = {
        |  Simple: (
        |    request: proto.com.example.service.persistence.IRequest,
        |    ctx: ActionCommandContext
        |  ) => proto.com.example.service.IResponse | Promise<proto.com.example.service.IResponse> | void | Promise<void>;
        |  StreamedIn: (
        |    ctx: ActionCommandContext & StreamedInCommandContext<proto.com.example.service.persistence.IRequest>
        |  ) => proto.com.example.service.IResponse | Promise<proto.com.example.service.IResponse> | void | Promise<void>;
        |  StreamedOut: (
        |    request: proto.com.example.service.persistence.IRequest,
        |    ctx: ActionCommandContext & StreamedOutCommandContext<proto.com.example.service.IResponse>
        |  ) => void | Promise<void>;
        |  FullyStreamed: (
        |    ctx: ActionCommandContext & StreamedInCommandContext<proto.com.example.service.persistence.IRequest> & StreamedOutCommandContext<proto.com.example.service.IResponse>
        |  ) => void | Promise<void>;
        |};
        |
        |export type MyService = TypedAction<CommandHandlers>;
        |""".stripMargin
    )
  }
}
