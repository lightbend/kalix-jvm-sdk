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
    override val descriptorSetPath: String = "codegen-annotation/descriptor-sets"

    def registry: ExtensionRegistry = {
      val reg = ExtensionRegistry.newInstance()
      reg.add(com.akkaserverless.Annotations.method)
      reg.add(com.akkaserverless.Annotations.codegen)
      reg
    }

  }
  object ServiceAnnotationConfig extends Config {
    override val label: String = "ServiceAnnotation"
    override val descriptorSetPath: String = "service-annotation/descriptor-sets"

    def registry: ExtensionRegistry = {
      val reg = ExtensionRegistry.newInstance()
      reg.add(com.akkaserverless.Annotations.service)
      reg.add(com.akkaserverless.Annotations.file)
      reg.add(com.akkaserverless.Annotations.method)
      reg
    }

  }
}
abstract class ModelBuilderSuite(val config: ModelBuilderSuite.Config) extends munit.FunSuite {
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

  def derivePackageForEntity(servicePackage: PackageNaming, domainPackage: PackageNaming): PackageNaming

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

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val derivedEntityPackage = derivePackageForEntity(shoppingCartPackage, domainPackage)

      val entity =
        ModelBuilder.EventSourcedEntity(
          // this is the name as defined in the proto file
          fullyQualifiedName("ShoppingCart", derivedEntityPackage),
          "shopping-cart",
          ModelBuilder.State(fullyQualifiedName("Cart", domainPackage)),
          List(
            ModelBuilder.Event(fullyQualifiedName("ItemAdded", domainPackage)),
            ModelBuilder.Event(fullyQualifiedName("ItemRemoved", domainPackage))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage))),
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

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val derivedEntityPackage = derivePackageForEntity(shoppingCartPackage, domainPackage)

      val entity = ModelBuilder.ValueEntity(
        fullyQualifiedName("ShoppingCart", derivedEntityPackage),
        "shopping-cart",
        ModelBuilder.State(fullyQualifiedName("Cart", domainPackage)))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "Create",
                fullyQualifiedName("CreateCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage))),
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

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val derivedEntityPackage = derivePackageForEntity(shoppingCartPackage, domainPackage)

      val entity = ModelBuilder.ReplicatedEntity(
        // this is the name as defined in the proto file
        fullyQualifiedName("ShoppingCart", derivedEntityPackage),
        "shopping-cart",
        ModelBuilder.ReplicatedCounterMap(
          ModelBuilder
            .TypeArgument("Product", domainPackage, TestData.guessDescriptor(domainPackage.name, domainPackage))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage))),
            entity.fqn.fullyQualifiedProtoName)))
    }.get
  }

  test(s"Action introspection (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/action-shoppingcart.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model =
        ModelBuilder
          .introspectProtobufClasses(descriptors)
          .copy(entities = Map.empty) // remove value entity, we don't need to assert it

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_controller_api.proto",
          "ShoppingcartControllerApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartController"),
          javaMultipleFiles = false)

      val derivedShoppingCartPackage = shoppingCartPackage.asJavaMultiFiles
      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartAction" ->
          ModelBuilder.ActionService(
            fullyQualifiedName("ShoppingCartAction", derivedShoppingCartPackage),
            List(
              command(
                "InitializeCart",
                fullyQualifiedName("NewCart", shoppingCartPackage),
                fullyQualifiedName("NewCartCreated", shoppingCartPackage)),
              command(
                "CreatePrePopulated",
                fullyQualifiedName("NewCart", shoppingCartPackage),
                fullyQualifiedName("NewCartCreated", shoppingCartPackage))),
            None)))
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

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/view/shopping_cart_view_model.proto",
          "ShoppingCartViewModel",
          "com.example.shoppingcart.view",
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val transformedUpdates =
        List(
          command(
            "ProcessAdded",
            fullyQualifiedName("ItemAdded", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)),
          command(
            "ProcessRemoved",
            fullyQualifiedName("ItemRemoved", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)),
          command(
            "ProcessCheckedOut",
            fullyQualifiedName("CheckedOut", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)))

      val queries = List(
        command(
          "GetCheckedOutCarts",
          fullyQualifiedName("GetCheckedOutCartsRequest", shoppingCartPackage),
          fullyQualifiedName("CartViewState", shoppingCartPackage),
          streamedOutput = true))

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.view.ShoppingCartViewService" ->
          ModelBuilder.ViewService(
            FullyQualifiedName(
              "ShoppingCartViewService",
              "ShoppingCartViewService",
              shoppingCartPackage.asJavaMultiFiles,
              TestData.guessDescriptor("ShoppingCartViewService", shoppingCartPackage)),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates,
            transformedUpdates,
            queries,
            None)))
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

    assertEquals(ModelBuilder.resolveFullName(pkg, "Test"), "com.example.Test")
    assertEquals(ModelBuilder.resolveFullName(pkg, ".sub.Test"), "com.example.sub.Test")
    assertEquals(ModelBuilder.resolveFullName(pkg, "other.package.Test"), "other.package.Test")
  }
}

class ModelBuilderWithCodegenAnnotationSuite extends ModelBuilderSuite(ModelBuilderSuite.CodegenAnnotationConfig) {

  test(s"EventSourcedEntity introspection with unnamed entity (${config.label})") {

    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/event-sourced-shoppingcart-unnamed.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity =
        ModelBuilder.EventSourcedEntity(
          // this is the name as defined in the proto file
          fullyQualifiedName("ShoppingCartServiceEntity", shoppingCartPackage).removeOuterClass,
          "shopping-cart",
          ModelBuilder.State(fullyQualifiedName("Cart", domainPackage)),
          List(
            ModelBuilder.Event(fullyQualifiedName("ItemAdded", domainPackage)),
            ModelBuilder.Event(fullyQualifiedName("ItemRemoved", domainPackage))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      // unnamed entities get their name derived from service name (name + Entity)
      // ShoppingCartService => ShoppingCartServiceEntity
      val generatedEntity = model.entities(entity.fqn.fullyQualifiedProtoName)
      assertEquals(generatedEntity.fqn.name, "ShoppingCartServiceEntity")
      assertEquals(generatedEntity.fqn.parent.protoPackage, "com.example.shoppingcart")

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" -> ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage))),
            entity.fqn.fullyQualifiedProtoName)))
    }.get
  }

  test(s"ValueEntity introspection with unnamed entity (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/value-shoppingcart-unnamed.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity = ModelBuilder.ValueEntity(
        fullyQualifiedName("ShoppingCartServiceEntity", shoppingCartPackage).removeOuterClass,
        "shopping-cart",
        ModelBuilder.State(fullyQualifiedName("Cart", domainPackage)))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      // unnamed entities get their name derived from service name (name + Entity)
      // ShoppingCartService => ShoppingCartServiceEntity
      val generatedEntity = model.entities(entity.fqn.fullyQualifiedProtoName)
      assertEquals(generatedEntity.fqn.name, "ShoppingCartServiceEntity")
      assertEquals(generatedEntity.fqn.parent.protoPackage, "com.example.shoppingcart")

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "Create",
                fullyQualifiedName("CreateCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage))),
            entity.fqn.fullyQualifiedProtoName)))
    }.get
  }

  test(s"ReplicatedEntity introspection with unnamed entity (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/replicated-shoppingcart-unnamed.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_api.proto",
          "ShoppingcartApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartApi"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      val googleEmptyPackage =
        PackageNaming(
          "Empty",
          "Empty",
          "google.protobuf",
          Some("com.google.protobuf"),
          Some("EmptyProto"),
          javaMultipleFiles = true)

      val entity = ModelBuilder.ReplicatedEntity(
        fullyQualifiedName("ShoppingCartServiceEntity", shoppingCartPackage).removeOuterClass,
        "shopping-cart",
        ModelBuilder.ReplicatedCounterMap(
          ModelBuilder
            .TypeArgument("Product", domainPackage, TestData.guessDescriptor(domainPackage.name, domainPackage))))

      assertEquals(model.entities, Map(entity.fqn.fullyQualifiedProtoName -> entity))

      // unnamed entities get their name derived from service name (name + Entity)
      // ShoppingCartService => ShoppingCartServiceEntity
      val generatedEntity = model.entities(entity.fqn.fullyQualifiedProtoName)
      assertEquals(generatedEntity.fqn.name, "ShoppingCartServiceEntity")
      assertEquals(generatedEntity.fqn.parent.protoPackage, "com.example.shoppingcart")

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartService" ->
          ModelBuilder.EntityService(
            fullyQualifiedName("ShoppingCartService", shoppingCartPackage),
            List(
              command(
                "AddItem",
                fullyQualifiedName("AddLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "RemoveItem",
                fullyQualifiedName("RemoveLineItem", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage)),
              command(
                "GetCart",
                fullyQualifiedName("GetShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Cart", shoppingCartPackage)),
              command(
                "RemoveCart",
                fullyQualifiedName("RemoveShoppingCart", shoppingCartPackage),
                fullyQualifiedName("Empty", googleEmptyPackage))),
            entity.fqn.fullyQualifiedProtoName)))
    }.get
  }

  // for Actions we must do the inverted test. Legacy test is unnamed, so we test the named case
  test(s"Action introspection with named action (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/action-shoppingcart-named.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.map(Descriptors.FileDescriptor.buildFrom(_, Array.empty, true))

      val model =
        ModelBuilder
          .introspectProtobufClasses(descriptors)
          .copy(entities = Map.empty) // remove value entity, we don't need to assert it

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/shoppingcart_controller_api.proto",
          "ShoppingcartControllerApi",
          "com.example.shoppingcart",
          None,
          Some("ShoppingCartController"),
          javaMultipleFiles = false)

      // Action components don't have a proto package.
      // So we need to build one for them since they are declared in a Service annotation,
      // we stay with this package, but we do resolve their FQN.
      // The package that the user asks may no be the same as the Service
      val derivedShoppingCartPackage = shoppingCartPackage.asJavaMultiFiles

      val userDefinedNamePackage =
        derivedShoppingCartPackage.copy(protoPackage = "com.example.shoppingcart.controllers")

      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.ShoppingCartAction" ->
          ModelBuilder.ActionService(
            fullyQualifiedName("ShoppingCartAction", derivedShoppingCartPackage),
            List(
              command(
                "InitializeCart",
                fullyQualifiedName("NewCart", shoppingCartPackage),
                fullyQualifiedName("NewCartCreated", shoppingCartPackage)),
              command(
                "CreatePrePopulated",
                fullyQualifiedName("NewCart", shoppingCartPackage),
                fullyQualifiedName("NewCartCreated", shoppingCartPackage))),
            // this is the name as defined in the proto file
            Some(fullyQualifiedName("ShoppingCartController", userDefinedNamePackage)))))
    }.get
  }

  // for Views we must do the inverted test. Legacy test is unnamed, so we test the named case
  test(s"View introspection with named view (${config.label})") {
    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("test-files").toURI)
    val descriptorFilePath =
      testFilesPath.resolve(config.descriptorSetPath + "/view-shoppingcart-named.desc")

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, config.registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors = fileList.foldLeft(List.empty[Descriptors.FileDescriptor])((acc, file) =>
        acc :+ Descriptors.FileDescriptor.buildFrom(file, acc.toArray, true))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)

      val shoppingCartPackage =
        PackageNaming(
          "com/example/shoppingcart/view/shopping_cart_view_model.proto",
          "ShoppingCartViewModel",
          "com.example.shoppingcart.view",
          None,
          Some("ShoppingCartViewModel"),
          javaMultipleFiles = false)

      val domainPackage =
        PackageNaming(
          "com/example/shoppingcart/domain/shoppingcart_domain.proto",
          "ShoppingcartDomain",
          "com.example.shoppingcart.domain",
          None,
          Some("ShoppingCartDomain"),
          javaMultipleFiles = false)

      // View components don't have a proto package.
      // So we need to build one for them since they are declared in a Service annotation,
      // we stay with this package.
      val derivedShoppingCartPackage = shoppingCartPackage.asJavaMultiFiles

      val userDefinedNamePackage =
        derivedShoppingCartPackage.copy(protoPackage = "com.example.shoppingcart.view")

      val transformedUpdates =
        List(
          command(
            "ProcessAdded",
            fullyQualifiedName("ItemAdded", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)),
          command(
            "ProcessRemoved",
            fullyQualifiedName("ItemRemoved", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)),
          command(
            "ProcessCheckedOut",
            fullyQualifiedName("CheckedOut", domainPackage),
            fullyQualifiedName("CartViewState", shoppingCartPackage)))

      val queries = List(
        command(
          "GetCheckedOutCarts",
          fullyQualifiedName("GetCheckedOutCartsRequest", shoppingCartPackage),
          fullyQualifiedName("CartViewState", shoppingCartPackage),
          streamedOutput = true))
      assertEquals(
        model.services,
        Map(
          "com.example.shoppingcart.view.ShoppingCartViewService" ->
          ModelBuilder.ViewService(
            FullyQualifiedName(
              "ShoppingCartViewService",
              "ShoppingCartViewService",
              derivedShoppingCartPackage,
              TestData.guessDescriptor("ShoppingCartViewService", shoppingCartPackage)),
            transformedUpdates ++ queries,
            "ShoppingCartViewService",
            transformedUpdates,
            transformedUpdates,
            queries,
            // this is the name as defined in the proto file
            Some(fullyQualifiedName("ShoppingCartView", userDefinedNamePackage)))))
    }.get
  }

  override def derivePackageForEntity(servicePackage: PackageNaming, domainPackage: PackageNaming): PackageNaming = {
    // Entities don't have a proto package so we need to build one for them
    // since they are declared in a Service annotation, we stay with this package,
    // but we do resolve their FQN. The package that the user asks may not be the same as the Service
    servicePackage.asJavaMultiFiles.changePackages(domainPackage.javaPackage)
  }

}

class ModelBuilderWithServiceAnnotationSuite extends ModelBuilderSuite(ModelBuilderSuite.ServiceAnnotationConfig) {

  override def derivePackageForEntity(servicePackage: PackageNaming, domainPackage: PackageNaming): PackageNaming = {
    // when using file annotation on a domain file,
    // the entity is considered to belong to the domain package
    domainPackage.asJavaMultiFiles
  }

}
