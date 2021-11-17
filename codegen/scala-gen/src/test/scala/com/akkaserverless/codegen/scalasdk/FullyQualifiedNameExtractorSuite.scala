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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest

import scala.collection.JavaConverters._
import com.lightbend.akkasls.codegen.{ DescriptorSet, Log, ModelBuilder }
import org.slf4j.LoggerFactory
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class FullyQualifiedNameExtractorSuite extends munit.FunSuite {
  val log = LoggerFactory.getLogger(getClass)
  implicit val codegenLog = new Log {
    override def debug(message: String): Unit = log.debug(message)
    override def info(message: String): Unit = log.info(message)
  }

  val d = DescriptorSet.descriptors(
    classOf[ClassNameSuite].getResourceAsStream("/test-files/descriptor-sets/value-shoppingcart.desc"))
  val descriptors = d.right.get.right.get.toList

  val api = descriptors.find(_.getName == "com/example/shoppingcart/shoppingcart_api.proto").get
  val apiCart = api.getMessageTypes.asScala.find(_.getName == "Cart").get

  val domain = descriptors.find(_.getName == "com/example/shoppingcart/domain/shoppingcart_domain.proto").get
  val stateCart = domain.getMessageTypes.asScala.find(_.getName == "Cart").get

  val di = DescriptorImplicits.fromCodeGenRequest(
    GeneratorParams(),
    CodeGenRequest(
      parameter = "",
      filesToGenerate = Seq.empty,
      allProtos = descriptors,
      compilerVersion = None,
      CodeGeneratorRequest.newBuilder().build()))
  implicit val fqnExtractor = new FullyQualifiedNameExtractor(di)

  test("extract api message types") {
    val fqn = fqnExtractor(apiCart)
    assertNoDiff(fqn.name, "Cart")
    assertNoDiff(fqn.parent.scalaPackage, "com.example.shoppingcart.shoppingcart_api")
  }

  test("extract domain state types") {
    val fqn = fqnExtractor(stateCart)
    assertNoDiff(fqn.name, "Cart")
    assertNoDiff(fqn.parent.scalaPackage, "com.example.shoppingcart.domain.shoppingcart_domain")
    assertNoDiff(
      fqn.descriptorImport.fullyQualifiedJavaName,
      "com.example.shoppingcart.domain.shoppingcart_domain.ShoppingcartDomainProto")
  }

  test("extract consistent types when used in the ModelBuilder") {
    val model = ModelBuilder.introspectProtobufClasses(descriptors)
    val shoppingCartValueEntity = model.entities.values.head.asInstanceOf[ModelBuilder.ValueEntity]

    assertNoDiff(shoppingCartValueEntity.state.fqn.parent.scalaPackage, fqnExtractor(stateCart).parent.scalaPackage)
  }

  test("find the filename of the Scala representation of the proto") {
    import di._
    val fileDescriptorObject = fqnExtractor.fileDescriptorObject(api)
    assertNoDiff(
      "com.example.shoppingcart.shoppingcart_api.ShoppingcartApiProto",
      fileDescriptorObject.fullyQualifiedJavaName)
  }
}
