/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk

import scala.annotation.nowarn

import kalix.codegen.scalasdk.impl.SourceGenerator
import com.google.protobuf.Descriptors
import kalix.codegen.{ ExampleSuiteBase, GeneratedFiles, ModelBuilder }
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class ExampleSuite extends ExampleSuiteBase {

  def regenerateAll: Boolean = false

  lazy val BuildInfo = kalix.codegen.scalasdk.BuildInfo

  override def propertyPath: String = "example.suite.scala.regenerate"

  @nowarn
  override def createMessageTypeExtractor(
      fileDescriptors: Seq[Descriptors.FileDescriptor]): ModelBuilder.ProtoMessageTypeExtractor =
    new ProtoMessageTypeExtractor(new DescriptorImplicits(GeneratorParams(flatPackage = true), fileDescriptors))

  override def generateFiles(model: ModelBuilder.Model): GeneratedFiles =
    SourceGenerator.generateFiles(model, Some("org.example"))

}
