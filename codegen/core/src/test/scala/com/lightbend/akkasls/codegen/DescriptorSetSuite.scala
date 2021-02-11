/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import java.nio.file.Paths

class DescriptorSetSuite extends munit.FunSuite {

  test("reading") {
    val testFilesPath  = Paths.get(getClass.getClassLoader.getResource("test-files").getFile)
    val descriptorFile = testFilesPath.resolve("descriptor-sets/hello-1.0-SNAPSHOT.protobin").toFile
    val result = DescriptorSet
      .fileDescriptors(descriptorFile)
      .flatMap(x => x.head.map(_.getServices.get(0).getFullName))
    assertEquals(result, Right("com.lightbend.MyServiceEntity"))
  }

  test("failed to open") {
    val result = DescriptorSet
      .fileDescriptors(Paths.get(".").toFile)
    assert(result.isLeft)
  }
}
