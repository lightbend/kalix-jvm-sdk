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

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors
import java.io.FileInputStream
import java.nio.file.Paths

import scala.jdk.CollectionConverters._
import scala.util.Using

import com.google.protobuf.ExtensionRegistry
import org.slf4j.LoggerFactory
import scala.collection.mutable

import com.lightbend.akkasls.codegen.TestData.fullyQualifiedName

class ModelBuilderSuite extends munit.FunSuite {
  val log = LoggerFactory.getLogger(getClass)
  implicit val codegenLog = new Log {
    override def debug(message: String): Unit = log.debug(message)
    override def info(message: String): Unit = log.info(message)
  }
  implicit val e = TestFullyQualifiedNameExtractor

  def command(
      name: String,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName,
      streamedInput: Boolean = false,
      streamedOutput: Boolean = false,
      inFromTopic: Boolean = false,
      outToTopic: Boolean = false) =
    ModelBuilder.Command(name, inputType, outputType, streamedInput, streamedOutput, inFromTopic, outToTopic)

  test("EventSourcedEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/event-sourced-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val derivedPackage = domainProto.copy(javaMultipleFiles = true)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity =
        ModelBuilder.EventSourcedEntity(
          fullyQualifiedName("ShoppingCart", derivedPackage),
          "eventsourced-shopping-cart",
          ModelBuilder.State(fullyQualifiedName("Cart", domainProto)),
          List(
            ModelBuilder.Event(fullyQualifiedName("ItemAdded", domainProto)),
            ModelBuilder.Event(fullyQualifiedName("ItemRemoved", domainProto))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartProto),
                fullyQualifiedName("Cart", shoppingCartProto))),
            entity.fqn.fullyQualifiedProtoName)))
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
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)
      val entity = ModelBuilder.ValueEntity(
        domainProto.protoPackage + "." + "ShoppingCart",
        fullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(fullyQualifiedName("Cart", domainProto)))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartProto),
                fullyQualifiedName("Cart", shoppingCartProto)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto))),
            entity.fqn.fullyQualifiedProtoName)))
    }.get
  }

  test("ReplicatedEntity introspection") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("descriptor-sets/replicated-shoppingcart.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val derivedPackage = domainProto.copy(javaMultipleFiles = true)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity = ModelBuilder.ReplicatedEntity(
        fullyQualifiedName("ShoppingCart", derivedPackage),
        "shopping-cart",
        ModelBuilder.ReplicatedCounterMap(
          ModelBuilder
            .TypeArgument("Product", domainProto, TestData.guessDescriptor(domainProto.name, domainProto))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartProto),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartProto),
                fullyQualifiedName("Cart", shoppingCartProto)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto))),
            entity.fqn.fullyQualifiedProtoName)))
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
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.foldLeft(List.empty[Descriptors.FileDescriptor])((acc, file) =>
        acc :+ Descriptors.FileDescriptor.buildFrom(file, acc.toArray, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "cart/shopping_cart_view_model.proto",
          "ShoppingCartViewModel",
          "shopping.cart.view",
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "cart/shopping_cart_domain.proto",
          "ShoppingCartDomain",
          "shopping.cart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyProto =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)
      val entity = ModelBuilder.ValueEntity(
        domainProto.protoPackage + "." + "ShoppingCart",
        fullyQualifiedName("ShoppingCart", domainProto),
        "shopping-cart",
        ModelBuilder.State(fullyQualifiedName("Cart", domainProto)))

      val transformedUpdates =
        List(
          command(
            "ProcessAdded",
            fullyQualifiedName("ItemAdded", domainProto),
            fullyQualifiedName("CartViewState", shoppingCartProto)),
          command(
            "ProcessRemoved",
            fullyQualifiedName("ItemRemoved", domainProto),
            fullyQualifiedName("CartViewState", shoppingCartProto)),
          command(
            "ProcessCheckedOut",
            fullyQualifiedName("CheckedOut", domainProto),
            fullyQualifiedName("CartViewState", shoppingCartProto)))
      val queries = List(
        command(
          "GetCheckedOutCarts",
          fullyQualifiedName("GetCheckedOutCartsRequest", shoppingCartProto),
          fullyQualifiedName("CartViewState", shoppingCartProto),
          streamedOutput = true))
      assertEquals(
        model.services,
        Map(
          "shopping.cart.view.ShoppingCartViewService" ->
          ModelBuilder.ViewService(
            FullyQualifiedName(
              "ShoppingCartViewService",
              "ShoppingCartViewService",
              shoppingCartProto,
              TestData.guessDescriptor("ShoppingCartViewService", shoppingCartProto)),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates,
            transformedUpdates)))
    }.get
  }

  test("deriving java package from proto options") {
    val protoFileName = "name.proto"
    val name = "Name"
    val pkg = "com.example"

    assertEquals(PackageNaming(protoFileName, name, pkg, None, None, javaMultipleFiles = false).javaPackage, pkg)
    assertEquals(
      PackageNaming(protoFileName, name, pkg, Some("override.package"), None, javaMultipleFiles = false).javaPackage,
      "override.package")
  }

  test("resolving full names") {
    val pkg = "com.example"

    assertEquals(ModelBuilder.resolveFullName("Test", pkg), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(".sub.Test", pkg), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName("other.package.Test", pkg), "other.package.Test")
  }
}
