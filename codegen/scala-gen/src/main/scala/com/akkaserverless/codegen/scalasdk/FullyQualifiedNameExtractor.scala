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

import com.google.protobuf.Descriptors
import com.lightbend.akkasls.codegen.{ FullyQualifiedName, ModelBuilder, PackageNaming }
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class FullyQualifiedNameExtractor(val di: DescriptorImplicits) extends ModelBuilder.FullyQualifiedNameExtractor {

  override def apply(descriptor: Descriptors.GenericDescriptor): FullyQualifiedName = {
    import di._

    val name = descriptor match {
      case d: Descriptors.Descriptor =>
        d.scalaType.name
      case _: Descriptors.MethodDescriptor =>
        throw new IllegalArgumentException("Cannot extract scala type for method, look at the service instead.")
      case s: Descriptors.ServiceDescriptor =>
        s.getName
    }

    FullyQualifiedName(name, packageName(descriptor))
  }

  override def packageName(descriptor: Descriptors.GenericDescriptor): PackageNaming = {
    import di._

    PackageNaming(
      descriptor.getFile.getName,
      descriptor.getName,
      descriptor.getFile.scalaPackage.fullName,
      None,
      None,
      None,
      false)
  }
}
object FullyQualifiedNameExtractor {
  def apply(request: CodeGenRequest): FullyQualifiedNameExtractor =
    new FullyQualifiedNameExtractor(descriptorImplicits(request))

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
