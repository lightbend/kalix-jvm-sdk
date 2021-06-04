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
        |    throw new Error("The update handler for `Created` is not implemented, yet");
        |  },
        |  Updated(event, state, ctx) {
        |    throw new Error("The update handler for `Updated` is not implemented, yet");
        |  }
        |});
        |
        |export default view;""".stripMargin
    )
  }
  test("source without transformations") {
    val protoRef = TestData.serviceProto()
    val service = TestData
      .simpleViewService(protoRef)
      .copy(
        transformedUpdates = List.empty
      )

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
        |export default view;""".stripMargin
    )
  }

  test("typedef source") {
    val protoRef = TestData.serviceProto()
    val service  = TestData.simpleViewService(protoRef)

    val sourceDoc =
      ViewServiceSourceGenerator.typedefSource(
        service
      )
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import {
        |  TypedView,
        |  ViewUpdateHandlerContext
        |} from "../akkaserverless";
        |import proto from "./proto";
        |
        |export type UpdateHandlers = {
        |  Created: (
        |    event: proto.com.example.service.persistence.IEntityCreated,
        |    state?: proto.com.example.service.IViewState,
        |    ctx: ViewUpdateHandlerContext
        |  ) => proto.com.example.service.IViewState;
        |  Updated: (
        |    event: proto.com.example.service.persistence.IEntityUpdated,
        |    state?: proto.com.example.service.IViewState,
        |    ctx: ViewUpdateHandlerContext
        |  ) => proto.com.example.service.IViewState;
        |};
        |
        |export type MyService = TypedView<UpdateHandlers>;
        |""".stripMargin
    )
  }
}
