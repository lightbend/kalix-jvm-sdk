/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import org.apache.commons.io.FileUtils

import java.nio.file.Files

class SourceGeneratorSuite extends munit.FunSuite {

  test("generate") {
    val sourceDirectory = Files.createTempDirectory("source-generator-test")
    try {

      val source1     = sourceDirectory.resolve("com/lightbend/MyService1.java")
      val sourceFile1 = source1.toFile
      FileUtils.forceMkdir(sourceFile1.getParentFile)
      FileUtils.touch(sourceFile1)

      val entities = List(
        ModelBuilder.EventSourcedEntity(
          Some("com/lightbend"),
          "MyEntity1",
          "com.lightbend.MyService1",
          List(
            ModelBuilder.Command("com.lightbend.MyService.Set", "com.lightbend.SetValue"),
            ModelBuilder.Command("com.lightbend.MyService.Get", "com.lightbend.GetValue")
          )
        ),
        ModelBuilder.EventSourcedEntity(
          Some("com/lightbend"),
          "MyEntity2",
          "com.lightbend.MyService2",
          List(
            ModelBuilder.Command("com.lightbend.MyService.Set", "com.lightbend.SetValue"),
            ModelBuilder.Command("com.lightbend.MyService.Get", "com.lightbend.GetValue")
          )
        )
      )

      val sources = SourceGenerator.generate(entities, sourceDirectory)

      assertEquals(Files.size(source1), 0L)
      assertEquals(sources, List(sourceDirectory.resolve("com/lightbend/MyService2.java")))
      assertEquals(Files.readAllBytes(sources.head).head.toChar, 'p')
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("source") {
    val entity = ModelBuilder.EventSourcedEntity(
      Some("com/lightbend"),
      "MyEntity2",
      "com.lightbend.MyService2",
      List(
        ModelBuilder.Command("com.lightbend.MyService.Set", "com.lightbend.SetValue"),
        ModelBuilder.Command("com.lightbend.MyService.Get", "com.lightbend.GetValue")
      )
    )
    val packageName = "com.lightbend"
    val className   = "MyService2"

    val sourceDoc = SourceGenerator.source(entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.lightbend;
      |
      |import com.google.protobuf.Empty;
      |import io.cloudstate.javasupport.EntityId;
      |import io.cloudstate.javasupport.eventsourced.*;
      |
      |/** An event sourced entity. */
      |@EventSourcedEntity
      |public class MyService2 {
      |    
      |}""".stripMargin
    )
  }
}
