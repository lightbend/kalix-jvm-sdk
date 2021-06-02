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

              val protoRef = TestData.serviceProto()

              val services = Map(
                "com.example.Service1" -> TestData.simpleEntityService(protoRef, "1"),
                "com.example.Service2" -> TestData.simpleEntityService(protoRef, "2"),
                "com.example.Service3" -> TestData.simpleEntityService(protoRef, "3")
              )

              val entities = Map(
                "com.example.Entity1" -> TestData.eventSourcedEntity("1"),
                "com.example.Entity2" -> TestData.valueEntity("2"),
                "com.example.Entity3" -> TestData.eventSourcedEntity("3")
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
      val protoRef = TestData.serviceProto()

      val services = Map(
        "com.example.Service1" -> TestData.simpleEntityService(protoRef, "1")
      )

      val entities = Map(
        "com.example.Entity1" -> TestData.eventSourcedEntity("1")
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

  test("generated component index source") {
    val protoRef = TestData.serviceProto()

    val generatedSourceDirectory = Paths.get("./generated/js")
    val sourceDirectory          = Paths.get("./src/js")

    val services = Map(
      "com.example.Service1" -> TestData.simpleEntityService(protoRef, "1"),
      "com.example.Service2" -> TestData.simpleEntityService(protoRef, "2"),
      "com.example.Service3" -> TestData.simpleEntityService(protoRef, "3"),
      "com.example.Service4" -> TestData.simpleViewService(protoRef, "4")
    )

    val entities = Map(
      "com.example.Entity1" -> TestData.eventSourcedEntity("1"),
      "com.example.Entity2" -> TestData.valueEntity("2"),
      "com.example.Entity3" -> TestData.eventSourcedEntity("3")
    )

    val sourceDoc =
      SourceGenerator.generatedComponentIndex(
        ModelBuilder.Model(services, entities),
        generatedSourceDirectory,
        sourceDirectory
      )

    assertEquals(
      sourceDoc.layout.replace("\\", "/"),
      """import myentity1 from "../../src/js/myentity1.js";
        |import myvalueentity2 from "../../src/js/myvalueentity2.js";
        |import myentity3 from "../../src/js/myentity3.js";
        |import myservice4 from "../../src/js/myservice4.js";
        |
        |export { myentity1, myvalueentity2, myentity3, myservice4 };
        |
        |export default [myentity1, myvalueentity2, myentity3, myservice4];""".stripMargin
    )
  }

  test("index source") {
    val generatedComponentIndexPath = Paths.get("./generated/my-generated-index.js")
    val sourceDirectory             = Paths.get("./src/js")

    val sourceDoc = SourceGenerator.indexSource(sourceDirectory, generatedComponentIndexPath)
    assertEquals(
      sourceDoc.layout.replace("\\", "/"),
      """import { AkkaServerless } from "@lightbend/akkaserverless-javascript-sdk";
        |import generatedComponents from "../../generated/my-generated-index.js";
        |
        |const server = new AkkaServerless();
        |generatedComponents.forEach(server.addComponent);
        |
        |server.start();""".stripMargin
    )
  }

}
