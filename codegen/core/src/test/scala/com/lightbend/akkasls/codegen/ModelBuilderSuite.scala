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

      val shoppingCartProto =
        ModelBuilder.ProtoReference(
          "shoppingcart.proto",
          "com.example.shoppingcart",
          Some(
            "github.com/lightbend/akkaserverless-go-sdk/example/shoppingcart;shoppingcart"
          ),
          None,
          Some("ShoppingCart")
        )

      val domainProto =
        ModelBuilder.ProtoReference(
          "persistence/domain.proto",
          "com.example.shoppingcart.persistence",
          Some(
            "github.com/lightbend/akkaserverless-go-sdk/example/shoppingcart/persistence;persistence"
          ),
          None,
          None
        )

      val googleEmptyProto =
        ModelBuilder.ProtoReference(
          "google.protobuf.Empty.placeholder.proto",
          "google.protobuf",
          None,
          None,
          None
        )

      assertEquals(
        entities,
        List(
          ModelBuilder.EventSourcedEntity(
            shoppingCartProto,
            "com.example.shoppingcart.ShoppingCartService",
            "ShoppingCartService",
            Some(ModelBuilder.TypeReference("Cart", domainProto)),
            List(
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.AddItem",
                ModelBuilder.TypeReference("AddLineItem", shoppingCartProto),
                ModelBuilder.TypeReference("Empty", googleEmptyProto)
              ),
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.RemoveItem",
                ModelBuilder.TypeReference("RemoveLineItem", shoppingCartProto),
                ModelBuilder.TypeReference("Empty", googleEmptyProto)
              ),
              ModelBuilder.Command(
                "com.example.shoppingcart.ShoppingCartService.GetCart",
                ModelBuilder.TypeReference("GetShoppingCart", shoppingCartProto),
                ModelBuilder.TypeReference("Cart", shoppingCartProto)
              )
            ),
            List(
              ModelBuilder.TypeReference("ItemAdded", domainProto),
              ModelBuilder.TypeReference("ItemRemoved", domainProto)
            )
          )
        )
      )
    }.get
  }

  test("deriving java package from proto options") {
    val fileName = "subdirectory/my_file.proto";
    val pkg      = "com.example"

    assertEquals(
      ModelBuilder
        .ProtoReference(fileName, pkg, None, None, None)
        .javaPackage,
      pkg
    )
    assertEquals(
      ModelBuilder
        .ProtoReference(fileName, pkg, None, Some("override.package"), None)
        .javaPackage,
      "override.package"
    )
  }

  test("resolving full names") {
    val pkg = "com.example"

    assertEquals(ModelBuilder.resolveFullName("Test", pkg), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(".sub.Test", pkg), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName("other.package.Test", pkg), "other.package.Test")
  }
}
