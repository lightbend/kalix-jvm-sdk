/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Files, Paths }
import org.apache.commons.io.FileUtils

class SourceGeneratorSuite extends munit.FunSuite {
  val protoRef =
    PackageNaming(
      "MyService",
      "com.example.service",
      None,
      None,
      None,
      false
    )
  test("generate") {
    val protoSourceDirectory = Files.createTempDirectory("proto-source-generator-test")
    try {
      val sourceDirectory = Files.createTempDirectory("source-generator-test")
      try {
        val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")
        try {
          val generatedSourceDirectory =
            Files.createTempDirectory("generated-source-generator-test")

          try {
            val protoSource1     = protoSourceDirectory.resolve("myservice1.proto")
            val protoSourceFile1 = protoSource1.toFile
            FileUtils.forceMkdir(protoSourceFile1.getParentFile)
            FileUtils.touch(protoSourceFile1)

            val source1     = sourceDirectory.resolve("myservice1.js")
            val sourceFile1 = source1.toFile
            FileUtils.forceMkdir(sourceFile1.getParentFile)
            FileUtils.touch(sourceFile1)
            val typedefSource1     = generatedSourceDirectory.resolve("myservice1.d.ts")
            val typedefSourceFile1 = typedefSource1.toFile
            FileUtils.forceMkdir(typedefSourceFile1.getParentFile)
            FileUtils.touch(typedefSourceFile1)

            val testSource2     = testSourceDirectory.resolve("myservice2.test.js")
            val testSourceFile2 = testSource2.toFile
            FileUtils.forceMkdir(testSourceFile2.getParentFile)
            FileUtils.touch(testSourceFile2)

            val entities = List(
              ModelBuilder.EventSourcedEntity(
                FullyQualifiedName("MyService1", protoRef),
                "MyService1",
                Some(ModelBuilder.State(FullyQualifiedName("State1", protoRef))),
                List(
                  ModelBuilder.Command(
                    FullyQualifiedName("Set", protoRef),
                    FullyQualifiedName("SetValue", protoRef),
                    FullyQualifiedName("Empty", protoRef)
                  ),
                  ModelBuilder.Command(
                    FullyQualifiedName("Get", protoRef),
                    FullyQualifiedName("GetValue", protoRef),
                    FullyQualifiedName("MyState", protoRef)
                  )
                ),
                List.empty
              ),
              ModelBuilder.EventSourcedEntity(
                FullyQualifiedName("MyService2", protoRef),
                "MyService2",
                Some(ModelBuilder.State(FullyQualifiedName("State2", protoRef))),
                List(
                  ModelBuilder.Command(
                    FullyQualifiedName("Set", protoRef),
                    FullyQualifiedName("SetValue", protoRef),
                    FullyQualifiedName("Empty", protoRef)
                  ),
                  ModelBuilder.Command(
                    FullyQualifiedName("Get", protoRef),
                    FullyQualifiedName("GetValue", protoRef),
                    FullyQualifiedName("MyState", protoRef)
                  )
                ),
                List.empty
              ),
              ModelBuilder.EventSourcedEntity(
                FullyQualifiedName("MyService3", protoRef),
                "MyService3",
                Some(ModelBuilder.State(FullyQualifiedName("State3", protoRef))),
                List(
                  ModelBuilder.Command(
                    FullyQualifiedName("Set", protoRef),
                    FullyQualifiedName("SetValue", protoRef),
                    FullyQualifiedName("Empty", protoRef)
                  ),
                  ModelBuilder.Command(
                    FullyQualifiedName("Get", protoRef),
                    FullyQualifiedName("GetValue", protoRef),
                    FullyQualifiedName("MyState", protoRef)
                  )
                ),
                List.empty
              )
            )

            val sources = SourceGenerator.generate(
              sourceDirectory.resolve("some.desc"),
              entities,
              protoSourceDirectory,
              sourceDirectory,
              testSourceDirectory,
              generatedSourceDirectory,
              "index.js"
            )

            assertEquals(Files.size(source1), 0L)
            assertEquals(Files.size(testSource2), 0L)

            assertEquals(
              sources,
              List(
                generatedSourceDirectory.resolve("myservice1.d.ts"),
                sourceDirectory.resolve("myservice2.js"),
                generatedSourceDirectory.resolve("myservice2.d.ts"),
                sourceDirectory.resolve("myservice3.js"),
                generatedSourceDirectory.resolve("myservice3.d.ts"),
                testSourceDirectory.resolve("myservice3.test.js"),
                sourceDirectory.resolve("index.js")
              )
            )

            // Test that all files are being written to (all files should start with at least one import)
            sources.foreach(source =>
              assertEquals(Files.readAllBytes(source).take(7).map(_.toChar).mkString, "import ")
            )
          } finally FileUtils.deleteDirectory(generatedSourceDirectory.toFile)
        } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
      } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(protoSourceDirectory.toFile)
  }

  test("source") {
    val entity = ModelBuilder.EventSourcedEntity(
      FullyQualifiedName("MyServiceEntity", protoRef),
      "MyServiceEntity",
      Some(ModelBuilder.State(FullyQualifiedName("MyState", protoRef))),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", protoRef),
          FullyQualifiedName("SetValue", protoRef),
          FullyQualifiedName("Empty", protoRef)
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", protoRef),
          FullyQualifiedName("GetValue", protoRef),
          FullyQualifiedName("MyState", protoRef)
        )
      ),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", protoRef))
      )
    )

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
        | * @typedef { import("../../lib/generated/myserviceentity").State } State
        | * 
        | * Event; the union of all possible event types
        | * @typedef { import("../../lib/generated/myserviceentity").Event } Event
        | * 
        | * MyServiceEntity; a strongly typed extension of EventSourcedEntity derived from your proto source
        | * @typedef { import("../../lib/generated/myserviceentity").MyServiceEntity } MyServiceEntity
        | */
        |
        |/**
        | * @type MyServiceEntity
        | */
        |const entity = new EventSourcedEntity(
        |  [
        |    "myentity1.proto",
        |    "someother.proto"
        |  ],
        |  "com.example.service.MyServiceEntity",
        |  "myserviceentity",
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
        |      throw ctx.fail("The command handler for `Set` is not implemented, yet");
        |    },
        |    Get(command, state, ctx) {
        |      throw ctx.fail("The command handler for `Get` is not implemented, yet");
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

  test("typedef source") {
    val googleKnownProto =
      PackageNaming(
        "Empty",
        "google.protobuf",
        None,
        None,
        None,
        true
      )
    val entity = ModelBuilder.EventSourcedEntity(
      FullyQualifiedName("MyServiceEntity", protoRef),
      "MyServiceEntity",
      Some(ModelBuilder.State(FullyQualifiedName("MyState", protoRef))),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", protoRef),
          FullyQualifiedName("SetValue", protoRef),
          FullyQualifiedName("Empty", googleKnownProto)
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", protoRef),
          FullyQualifiedName("GetValue", protoRef),
          FullyQualifiedName("MyState", protoRef)
        )
      ),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", protoRef))
      )
    )

    val sourceDoc =
      SourceGenerator.typedefSource(
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
        |export type State = proto.com.example.service.IMyState;
        |export type Event = proto.com.example.service.ISetEvent;
        |export type Command =
        |  | proto.com.example.service.ISetValue
        |  | proto.com.example.service.IGetValue;
        |
        |export type EventHandlers = {
        |  SetEvent: (
        |    event: proto.com.example.service.ISetEvent,
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
        |export type MyServiceEntity = TypedEventSourcedEntity<
        |  State,
        |  EventHandlers,
        |  CommandHandlers
        |>;
        |""".stripMargin
    )
  }

  test("test source") {
    val entity =
      ModelBuilder.EventSourcedEntity(
        FullyQualifiedName("MyService1", protoRef),
        "MyService1",
        Some(ModelBuilder.State(FullyQualifiedName("MyState", protoRef))),
        List(
          ModelBuilder.Command(
            FullyQualifiedName("Set", protoRef),
            FullyQualifiedName("SetValue", protoRef),
            FullyQualifiedName("protobuf.Empty", protoRef)
          ),
          ModelBuilder.Command(
            FullyQualifiedName("Get", protoRef),
            FullyQualifiedName("GetValue", protoRef),
            FullyQualifiedName("MyState", protoRef)
          )
        ),
        List.empty
      )

    val testSourceDirectory = Paths.get("./test/js");
    val sourceDirectory     = Paths.get("./src/js");
    val sourceDoc           = SourceGenerator.testSource(entity, testSourceDirectory, sourceDirectory)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """import { MockEventSourcedEntity } from "./testkit.js";
        |import { expect } from "chai";
        |import myservice1 from "../../src/js/myservice1.js";
        |
        |describe("MyService1", () => {
        |  const entityId = "entityId";
        |  
        |  describe("Set", () => {
        |    it("should...", () => {
        |      const entity = new MockEventSourcedEntity(myservice1, entityId);
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
        |      const entity = new MockEventSourcedEntity(myservice1, entityId);
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

  test("index source") {
    val entities = List(
      ModelBuilder.EventSourcedEntity(
        FullyQualifiedName("MyService1", protoRef),
        "MyService1",
        Some(ModelBuilder.State(FullyQualifiedName("MyState", protoRef))),
        List(
          ModelBuilder.Command(
            FullyQualifiedName("Set", protoRef),
            FullyQualifiedName("SetValue", protoRef),
            FullyQualifiedName("protobuf.Empty", protoRef)
          ),
          ModelBuilder.Command(
            FullyQualifiedName("Get", protoRef),
            FullyQualifiedName("GetValue", protoRef),
            FullyQualifiedName("MyState", protoRef)
          )
        ),
        List.empty
      )
    )

    val sourceDoc = SourceGenerator.indexSource(entities)
    assertEquals(
      sourceDoc.layout,
      """import myservice1 from "./myservice1.js";
        |
        |myservice1.start();""".stripMargin
    )
  }
}
