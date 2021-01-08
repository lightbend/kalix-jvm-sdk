/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import java.nio.file.Paths
import java.nio.file.Files

class ModelBuilderSuite extends munit.FunSuite {

  test("compilation") {
    val testFilesPath        = Paths.get(getClass().getClassLoader().getResource("test-files").getFile())
    val protoSourceDirectory = testFilesPath.resolve("generated-sources/protobuf/java")
    val sourcePaths          = ModelBuilder.collectProtobufSources(protoSourceDirectory)
    val outputDirectory      = Files.createTempDirectory("compileProtobufSources")
    outputDirectory.toFile().deleteOnExit();
    val status = ModelBuilder.compileProtobufSources(sourcePaths, outputDirectory)
    assertEquals(0, status)
    val classPaths =
      ModelBuilder.mapProtobufClasses(protoSourceDirectory, sourcePaths, outputDirectory).toList
    assertEquals(2, classPaths.size)
    assert(classPaths(0).toFile().exists())
    assert(classPaths(1).toFile().exists())
  }

  test("filtering") {
    val source1 = Files.createTempFile("source1", ".java")
    source1.toFile().deleteOnExit()
    val source2 = Files.createTempFile("source2", ".java")
    source2.toFile().deleteOnExit()
    val source3 = Files.createTempFile("source3", ".java")
    source3.toFile().deleteOnExit()

    val class1 = Paths.get(source1.toString().replace(".java", ".class"))
    val class2 = Paths.get(source2.toString().replace(".java", ".class"))
    val class3 = Paths.get(source3.toString().replace(".java", ".class"))

    assert(class1.toFile().createNewFile())
    class1.toFile().deleteOnExit()

    assert(class2.toFile().createNewFile())
    class2.toFile().setLastModified(0)
    class2.toFile().deleteOnExit()

    val filteredSources =
      ModelBuilder.filterNewProtobufSources(
        List(source1, source2, source3),
        List(class1, class2, class3)
      )

    assertEquals(filteredSources, List(source2, source3))
  }

  test("introspection") {
    val testFilesPath    = Paths.get(getClass().getClassLoader().getResource("test-files").getFile())
    val classesDirectory = testFilesPath.resolve("classes")
    val class1           = classesDirectory.resolve("com/lightbend/MyEntity.class")
    ModelBuilder.introspectProtobufClasses(classesDirectory, List(class1))
  }
}
