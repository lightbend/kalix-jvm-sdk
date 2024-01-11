/*
 * Copyright 2024 Lightbend Inc.
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
