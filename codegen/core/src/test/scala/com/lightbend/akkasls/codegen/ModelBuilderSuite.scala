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

object ModelBuilderSuite {
  trait Config {
    def label: String
    def registry: ExtensionRegistry
    def descriptorSetPath: String
  }
  object CodegenAnnotationConfig extends Config {
    override val label: String = "CodegenAnnotation"
    override val descriptorSetPath: String = "descriptor-sets-new-format"

    def registry: ExtensionRegistry = {
      val reg = ExtensionRegistry.newInstance()
      reg.add(com.akkaserverless.codegen.Annotations.codegen)
      reg.add(com.akkaserverless.Annotations.method)
      reg
    }

  }
  object ServiceAnnotationConfig extends Config {
    override val label: String = "ServiceAnnotation"
    override val descriptorSetPath: String = "descriptor-sets"

    def registry: ExtensionRegistry = {
      val reg = ExtensionRegistry.newInstance()
      reg.add(com.akkaserverless.Annotations.service)
      reg.add(com.akkaserverless.Annotations.file)
      reg.add(com.akkaserverless.Annotations.method)
      reg
    }

  }
}
abstract class ModelBuilderSuite(config: ModelBuilderSuite.Config) extends munit.FunSuite {
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

  test(s"EventSourcedEntity introspection (${config.label})") {

    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/event-sourced-shoppingcart.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
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
          "shopping-cart",
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

  test(s"ValueEntity introspection (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/value-shoppingcart.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
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
                "Create",
                fullyQualifiedName("CreateCart", shoppingCartProto),
                fullyQualifiedName("Empty", googleEmptyProto)),
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

  test(s"ReplicatedEntity introspection (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/replicated-shoppingcart.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
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

  test(s"View introspection (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/view-shoppingcart.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.foldLeft(List.empty[Descriptors.FileDescriptor])((acc, file) =>
        acc :+ Descriptors.FileDescriptor.buildFrom(file, acc.toArray, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartProto =
        PackageNaming(
          "com/example/shoppingcart/view/shopping_cart_view_model.proto",
          "ShoppingCartViewModel",
          "com.example.shoppingcart.view",
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false)

      val domainProto =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
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
          "com.example.shoppingcart.view.ShoppingCartViewService" ->
          ModelBuilder.ViewService(
            FullyQualifiedName(
              "ShoppingCartViewService",
              "ShoppingCartViewService",
              shoppingCartProto,
              TestData.guessDescriptor("ShoppingCartViewService", shoppingCartProto)),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates,
            transformedUpdates,
            queries)))
    }.get
  }

  test(s"deriving java package from proto options (${config.label})") {
    val protoFileName = "name.proto"
    val name = "Name"
    val pkg = "com.example"

    assertEquals(PackageNaming(protoFileName, name, pkg, None, None, javaMultipleFiles = false).javaPackage, pkg)
    assertEquals(
      PackageNaming(protoFileName, name, pkg, Some("override.package"), None, javaMultipleFiles = false).javaPackage,
      "override.package")
  }

  test(s"resolving full names  (${config.label})") {
    val pkg = "com.example"

    assertEquals(ModelBuilder.resolveFullName("Test", pkg), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(".sub.Test", pkg), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName("other.package.Test", pkg), "other.package.Test")
  }
}

class ModelBuilderWithCodegenAnnotationSuite extends ModelBuilderSuite(ModelBuilderSuite.CodegenAnnotationConfig)
class ModelBuilderWithServiceAnnotationSuite extends ModelBuilderSuite(ModelBuilderSuite.ServiceAnnotationConfig)
