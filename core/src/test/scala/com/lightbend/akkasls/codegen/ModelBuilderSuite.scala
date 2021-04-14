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
        PackageNaming(
          "Shoppingcart",
          "com.example.shoppingcart",
          Some(
            "github.com/lightbend/akkaserverless-go-sdk/example/shoppingcart;shoppingcart"
          ),
          None,
          Some("ShoppingCart"),
          false
        )

      val domainProto =
        PackageNaming(
          "Domain",
          "com.example.shoppingcart.persistence",
          Some(
            "github.com/lightbend/akkaserverless-go-sdk/example/shoppingcart/persistence;persistence"
          ),
          None,
          None,
          false
        )

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          true
        )

      assertEquals(
        entities,
        List(
          ModelBuilder.EventSourcedEntity(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            "ShoppingCartService",
            Some(ModelBuilder.State(FullyQualifiedName("Cart", domainProto))),
            List(
              ModelBuilder.Command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)
              ),
              ModelBuilder.Command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto)
              ),
              ModelBuilder.Command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto)
              )
            ),
            List(
              ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
              ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))
            )
          )
        )
      )
    }.get
  }

  test("deriving java package from proto options") {
    val name = "Name"
    val pkg  = "com.example"

    assertEquals(
      PackageNaming(name, pkg, None, None, None, false).javaPackage,
      pkg
    )
    assertEquals(
      PackageNaming(name, pkg, None, Some("override.package"), None, false).javaPackage,
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
