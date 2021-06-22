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

import scala.collection.mutable

class ModelBuilderSuite extends munit.FunSuite {
  test("EventSourcedEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/event-sourced-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList    = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(
        descriptors
      )

      val shoppingCartProto =
        PackageNaming(
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false
        )

      val domainProto =
        PackageNaming(
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false
        )

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true
        )

      val entity =
        ModelBuilder.EventSourcedEntity(
          FullyQualifiedName("ShoppingCart", domainProto),
          "eventsourced-shopping-cart",
          Some(ModelBuilder.State(FullyQualifiedName("Cart", domainProto))),
          List(
            ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
            ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))
          )
        )

      assertEquals(
        model.entities,
        Map(entity.fqn.fullName -> entity)
      )

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              ModelBuilder.Command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto),
                streamedInput = false,
                streamedOutput = false
              ),
              ModelBuilder.Command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto),
                streamedInput = false,
                streamedOutput = false
              ),
              ModelBuilder.Command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto),
                streamedInput = false,
                streamedOutput = false
              )
            ),
            entity.fqn.fullName
          )
        )
      )
    }.get
  }

  test("ValueEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/value-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList    = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(
        descriptors
      )

      val shoppingCartProto =
        PackageNaming(
          "ShoppingcartApi",
          "com.example.valueentity.shoppingcart",
          None,
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false
        )

      val domainProto =
        PackageNaming(
          "ShoppingcartDomain",
          "com.example.valueentity.shoppingcart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false
        )

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true
        )
      val entity = ModelBuilder.ValueEntity(
        FullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(FullyQualifiedName("Cart", domainProto))
      )

      assertEquals(
        model.entities,
        Map(entity.fqn.fullName -> entity)
      )

      assertEquals(
        model.services,
        Map(
          "com.example.valueentity.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            FullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              ModelBuilder.Command(
                FullyQualifiedName("AddItem", shoppingCartProto),
                FullyQualifiedName("AddLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto),
                streamedInput = false,
                streamedOutput = false
              ),
              ModelBuilder.Command(
                FullyQualifiedName("RemoveItem", shoppingCartProto),
                FullyQualifiedName("RemoveLineItem", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto),
                streamedInput = false,
                streamedOutput = false
              ),
              ModelBuilder.Command(
                FullyQualifiedName("GetCart", shoppingCartProto),
                FullyQualifiedName("GetShoppingCart", shoppingCartProto),
                FullyQualifiedName("Cart", shoppingCartProto),
                streamedInput = false,
                streamedOutput = false
              ),
              ModelBuilder.Command(
                FullyQualifiedName("RemoveCart", shoppingCartProto),
                FullyQualifiedName("RemoveShoppingCart", shoppingCartProto),
                FullyQualifiedName("Empty", googleEmptyProto),
                streamedInput = false,
                streamedOutput = false
              )
            ),
            entity.fqn.fullName
          )
        )
      )
    }.get
  }

  test("View introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/view-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)
    registry.add(com.akkaserverless.Annotations.method)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList    = fileDescSet.getFileList.asScala

      val descriptors = fileList.foldLeft(List.empty[Descriptors.FileDescriptor])((acc, file) =>
        acc :+ Descriptors.FileDescriptor.buildFrom(file, acc.toArray, true)
      )

      val model = ModelBuilder.introspectProtobufClasses(
        descriptors
      )

      val shoppingCartProto =
        PackageNaming(
          "ShoppingCartViewModel",
          "shopping.cart.view",
          None,
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false
        )

      val domainProto =
        PackageNaming(
          "ShoppingCartDomain",
          "shopping.cart.domain",
          None,
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false
        )

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "google.protobuf",
          Some("google.golang.org/protobuf/types/known/emptypb"),
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true
        )
      val entity = ModelBuilder.ValueEntity(
        FullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(FullyQualifiedName("Cart", domainProto))
      )

      val transformedUpdates =
        List(
          ModelBuilder.Command(
            FullyQualifiedName("ProcessAdded", shoppingCartProto),
            FullyQualifiedName("ItemAdded", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto),
            streamedInput = false,
            streamedOutput = false
          ),
          ModelBuilder.Command(
            FullyQualifiedName("ProcessRemoved", shoppingCartProto),
            FullyQualifiedName("ItemRemoved", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto),
            streamedInput = false,
            streamedOutput = false
          ),
          ModelBuilder.Command(
            FullyQualifiedName("ProcessCheckedOut", shoppingCartProto),
            FullyQualifiedName("CheckedOut", domainProto),
            FullyQualifiedName("CartViewState", shoppingCartProto),
            streamedInput = false,
            streamedOutput = false
          )
        )
      val queries = List(
        ModelBuilder.Command(
          FullyQualifiedName("GetCheckedOutCarts", shoppingCartProto),
          FullyQualifiedName("GetCheckedOutCartsRequest", shoppingCartProto),
          FullyQualifiedName("CartViewState", shoppingCartProto),
          streamedInput = false,
          streamedOutput = true
        )
      )
      assertEquals(
        model.services,
        Map(
          "shopping.cart.view.ShoppingCartViewService" ->
          ModelBuilder.ViewService(
            FullyQualifiedName("ShoppingCartViewService", shoppingCartProto),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates
          )
        )
      )
    }.get
  }

  test("deriving java package from proto options") {
    val name = "Name"
    val pkg  = "com.example"

    assertEquals(
      PackageNaming(name, pkg, None, None, None, javaMultipleFiles = false).javaPackage,
      pkg
    )
    assertEquals(
      PackageNaming(
        name,
        pkg,
        None,
        Some("override.package"),
        None,
        javaMultipleFiles = false
      ).javaPackage,
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
