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
import com.google.protobuf.ExtensionRegistry

class ModelBuilderSuite extends munit.FunSuite {

  test("introspection") {
    val testFilesPath      = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath = testFilesPath.resolve("descriptor-sets/user-function.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList    = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val entities = ModelBuilder.introspectProtobufClasses(
        descriptors
      )

      assertEquals(
        entities,
        List(
          ModelBuilder.EventSourcedEntity(
            Some("github.com/lightbend/akkaserverless-go-sdk/example/shoppingcart;shoppingcart"),
            Some("ShoppingCart"),
            "com.example.shoppingcart.ShoppingCartService",
            "ShoppingCartService",
            Some("com.example.shoppingcart.persistence.Cart"),
            List(
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.AddItem",
                "com.example.shoppingcart.AddLineItem",
                "google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.RemoveItem",
                "com.example.shoppingcart.RemoveLineItem",
                "google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.GetCart",
                "com.example.shoppingcart.GetShoppingCart",
                "com.example.shoppingcart.Cart"
              )
            ),
            List(
              "com.example.shoppingcart.persistence.ItemAdded",
              "com.example.shoppingcart.persistence.ItemRemoved"
            )
          )
        )
      )
    }.get
  }

  test("resolving full names") {
    val pkg = "com.example"

    assertEquals(ModelBuilder.resolveFullName("Test", pkg), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(".sub.Test", pkg), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName("other.package.Test", pkg), "other.package.Test")
  }
}
