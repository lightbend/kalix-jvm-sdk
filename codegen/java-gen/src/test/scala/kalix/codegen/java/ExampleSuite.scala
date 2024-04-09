/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import com.google.protobuf.Descriptors
import kalix.codegen.{ ExampleSuiteBase, GeneratedFiles, ModelBuilder }

class ExampleSuite extends ExampleSuiteBase {

  def regenerateAll: Boolean = false

  lazy val BuildInfo = kalix.codegen.java.BuildInfo

  override def propertyPath: String = "example.suite.java.regenerate"

  override def createMessageTypeExtractor(
      fileDescriptors: Seq[Descriptors.FileDescriptor]): ModelBuilder.ProtoMessageTypeExtractor =
    ProtoMessageTypeExtractor

  override def generateFiles(model: ModelBuilder.Model): GeneratedFiles =
    SourceGenerator.generateFiles(model, "org.example.Main")

}
