/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors

import java.io.FileInputStream
import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import scala.util.Using

class ModelBuilderSuite extends munit.FunSuite {

  test("introspection") {
    val testFilesPath      = Paths.get(getClass.getClassLoader.getResource("test-files").getFile)
    val descriptorFilePath = testFilesPath.resolve("descriptor-sets/hello-1.0-SNAPSHOT.protobin")
    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val descriptors = FileDescriptorSet.parseFrom(fis).getFileList.asScala
      val descriptor  = Descriptors.FileDescriptor.buildFrom(descriptors.head, Array.empty, true)
      val entities = ModelBuilder.introspectProtobufClasses(
        descriptor,
        ".*Service"
      )

      assertEquals(
        entities,
        List(
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity"),
            "com.lightbend.MyService",
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            )
          )
        )
      )
    }
  }
}
