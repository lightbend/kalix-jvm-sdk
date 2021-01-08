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
}
