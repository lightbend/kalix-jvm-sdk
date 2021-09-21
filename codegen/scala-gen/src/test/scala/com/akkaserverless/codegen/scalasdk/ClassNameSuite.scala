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

package com.akkaserverless.codegen.scalasdk

import com.google.protobuf.CodedInputStream
import com.google.protobuf.descriptor.FileDescriptorProto
import com.lightbend.akkasls.codegen.DescriptorSet
import scalapb.descriptors.FileDescriptor

class ClassNameSuite extends munit.FunSuite {
  test("parse a proto") {
    val d = DescriptorSet.descriptors(
      classOf[ClassNameSuite].getResourceAsStream("/test-files/descriptor-sets/value-shoppingcart.desc"))
    val descriptors = d.right.get.right.get.toList
    assertEquals(descriptors.size, 10)

    val api = descriptors.find(_.getName == "shoppingcart_api.proto").get
    assertEquals(api.getServices.size, 1)
    assertEquals(api.getMessageTypes.size, 6)

  }
}
