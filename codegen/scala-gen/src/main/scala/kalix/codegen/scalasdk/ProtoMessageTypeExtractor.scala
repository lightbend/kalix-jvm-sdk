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

package kalix.codegen.scalasdk

import com.google.protobuf.Descriptors
import kalix.codegen.{ ModelBuilder, PackageNaming, ProtoMessageType }
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class ProtoMessageTypeExtractor(val di: DescriptorImplicits) extends ModelBuilder.ProtoMessageTypeExtractor {
  import di._

  override def apply(descriptor: Descriptors.GenericDescriptor): ProtoMessageType = {
    val name = descriptor match {
      case d: Descriptors.Descriptor =>
        d.scalaType.name
      case _: Descriptors.MethodDescriptor =>
        throw new IllegalArgumentException("Cannot extract scala type for method, look at the service instead.")
      case s: Descriptors.ServiceDescriptor =>
        s.getName
    }

    ProtoMessageType(name, name, packageName(descriptor), Some(fileDescriptorObject(descriptor.getFile)))
  }

  override def packageName(descriptor: Descriptors.GenericDescriptor): PackageNaming =
    PackageNaming(
      descriptor.getFile.getName,
      descriptor.getName,
      descriptor.getFile.getPackage,
      Some(descriptor.getFile.scalaPackage.fullName),
      None,
      javaMultipleFiles = false)

  def packageName(protoFileName: String, scalaName: ScalaName): PackageNaming =
    PackageNaming(
      protoFileName,
      scalaName.name,
      scalaName.fullName.split("\\.").init.mkString("."),
      None,
      None,
      javaMultipleFiles = false)

  override def fileDescriptorObject(descriptor: Descriptors.GenericDescriptor): ProtoMessageType =
    ProtoMessageType.noDescriptor(
      descriptor.getFile.fileDescriptorObject.name,
      packageName(descriptor.getName, descriptor.getFile.fileDescriptorObject))
}
object ProtoMessageTypeExtractor {
  def apply(request: CodeGenRequest): ProtoMessageTypeExtractor =
    new ProtoMessageTypeExtractor(descriptorImplicits(request))

  def descriptorImplicits(request: CodeGenRequest): DescriptorImplicits = {
    val params =
      request.parameter.split(",").map(_.trim).filter(_.nonEmpty).foldLeft[GeneratorParams](GeneratorParams()) {
        case (p, "java_conversions")            => p.copy(javaConversions = true)
        case (p, "flat_package")                => p.copy(flatPackage = true)
        case (p, "single_line_to_string")       => p.copy(singleLineToProtoString = true) // for backward-compatibility
        case (p, "single_line_to_proto_string") => p.copy(singleLineToProtoString = true)
        case (p, "ascii_format_to_string")      => p.copy(asciiFormatToString = true)
        case (p, "no_lenses")                   => p.copy(lenses = false)
        case (p, "retain_source_code_info")     => p.copy(retainSourceCodeInfo = true)
        case (p, "grpc")                        => p.copy(grpc = true)
        case (x, _)                             => x
      }
    DescriptorImplicits.fromCodeGenRequest(params, request)
  }
}
