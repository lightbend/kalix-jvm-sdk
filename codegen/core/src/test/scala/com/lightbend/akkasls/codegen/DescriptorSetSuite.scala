/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.akkasls.codegen

import java.nio.file.Paths

class DescriptorSetSuite extends munit.FunSuite {

  test("reading") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFile = testFilesPath.resolve("descriptor-sets/hello-1.0-SNAPSHOT.protobin").toFile
    val result = DescriptorSet
      .fileDescriptors(descriptorFile) match {
      case Right(fileExists) =>
        fileExists match {
          case Right(descriptors) => descriptors.map(_.getServices.get(0).getFullName)
        }
    }
    assertEquals(result, List("com.lightbend.MyServiceEntity"))
  }

  test("failed to open") {
    val result = DescriptorSet
      .fileDescriptors(Paths.get(".").toFile)
    assert(result.isLeft)
  }
}
