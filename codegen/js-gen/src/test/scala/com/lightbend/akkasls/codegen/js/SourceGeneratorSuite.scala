/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import java.nio.file.{ Files, Paths }
import org.apache.commons.io.FileUtils

class SourceGeneratorSuite extends munit.FunSuite {
  test("generate") {
    val sourceDirectory = Files.createTempDirectory("source-generator-test")
    try {

      val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")

      try {

        val source1     = sourceDirectory.resolve("myservice1.js")
        val sourceFile1 = source1.toFile
        FileUtils.forceMkdir(sourceFile1.getParentFile)
        FileUtils.touch(sourceFile1)

        val testSource2     = testSourceDirectory.resolve("myservice2.test.js")
        val testSourceFile2 = testSource2.toFile
        FileUtils.forceMkdir(testSourceFile2.getParentFile)
        FileUtils.touch(testSourceFile2)

        val entities = List(
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity1"),
            "com.lightbend.MyService1",
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            )
          ),
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity2"),
            "com.lightbend.MyService2",
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            )
          ),
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity3"),
            "com.lightbend.something.MyService3",
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            )
          )
        )

        val sources = SourceGenerator.generate(
          sourceDirectory.resolve("some.desc"),
          entities,
          sourceDirectory,
          sourceDirectory,
          testSourceDirectory,
          "index.js"
        )

        assertEquals(Files.size(source1), 0L)
        assertEquals(Files.size(testSource2), 0L)

        assertEquals(
          sources,
          List(
            sourceDirectory.resolve("myservice2.js"),
            sourceDirectory.resolve("myservice3.js"),
            testSourceDirectory.resolve("myservice3.test.js"),
            sourceDirectory.resolve("index.js")
          )
        )

        // Test that the main, source and test files are being written to
        assertEquals(Files.readAllBytes(sources.head).head.toChar, 'c')
        assertEquals(Files.readAllBytes(sources.drop(1).head).head.toChar, 'c')
        assertEquals(Files.readAllBytes(sources.drop(3).head).head.toChar, 'r')

      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("source") {
    val entity = ModelBuilder.EventSourcedEntity(
      Some("com/lightbend"),
      Some("MyEntity"),
      "com.lightbend.MyServiceEntity",
      List(
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Set",
          "com.lightbend.SetValue",
          "google.protobuf.Empty"
        ),
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Get",
          "com.lightbend.GetValue",
          "com.lightbend.MyState"
        )
      )
    )

    val protobufSourceDirectory = Paths.get("./src/proto")
    val sourceDirectory         = Paths.get("./src/js")

    val sourceDoc =
      SourceGenerator.source(protobufSourceDirectory, sourceDirectory, entity)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"), // Cope with windows testing
      """const EventSourced = require("cloudstate").EventSourced;
        |
        |const entity = new EventSourced(
        |  ["myserviceentity.proto"],
        |  "com.lightbend.MyServiceEntity",
        |  {
        |    includeDirs: ["../proto"],
        |    persistenceId: "myserviceentity"
        |  }
        |);
        |
        |entity.setBehavior(state => {
        |  return {
        |    commandHandlers: {
        |      Set: set,
        |      Get: get
        |    }
        |  };
        |});
        |
        |function set(command, state, ctx) {
        |  ctx.fail("The command handler for `Set` is not implemented, yet");
        |}
        |
        |function get(command, state, ctx) {
        |  ctx.fail("The command handler for `Get` is not implemented, yet");
        |}
        |
        |module.exports = entity;""".stripMargin
    )
  }

  test("index source") {
    val entities = List(
      ModelBuilder.EventSourcedEntity(
        Some("com/lightbend"),
        Some("MyEntity1"),
        "com.lightbend.MyService1",
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            "com.lightbend.SetValue",
            "com.google.protobuf.Empty"
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            "com.lightbend.GetValue",
            "com.lightbend.MyState"
          )
        )
      )
    )

    val sourceDoc = SourceGenerator.indexSource(entities)
    assertEquals(
      sourceDoc.layout,
      """require("./myservice1.js").start();"""
    )
  }
}
