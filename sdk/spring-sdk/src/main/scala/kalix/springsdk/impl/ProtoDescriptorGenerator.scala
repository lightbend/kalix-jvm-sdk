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

package kalix.springsdk.impl

import com.google.api.{ AnnotationsProto => HttpAnnotationsProto }
import com.google.protobuf.AnyProto
import com.google.protobuf.{ Any => JavaPbAny }
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import kalix.{ Annotations => KalixAnnotations }

object ProtoDescriptorGenerator {

  // do we need to recurse into the dependencies of the dependencies? Probably not, just top level imports.
  val dependencies: Array[Descriptors.FileDescriptor] =
    Array(AnyProto.getDescriptor, HttpAnnotationsProto.getDescriptor, KalixAnnotations.getDescriptor)

  def genFileDescriptor(
      name: String,
      packageName: String,
      service: DescriptorProtos.ServiceDescriptorProto,
      messages: Seq[DescriptorProtos.DescriptorProto]): Descriptors.FileDescriptor = {

    val protoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder
    protoBuilder
      .setName(name + ".proto") // FIXME: snake_case this ?!
      .setSyntax("proto3")
      .setPackage(packageName)
      .setOptions(DescriptorProtos.FileOptions.newBuilder.setJavaMultipleFiles(true).build)

    protoBuilder.addDependency("google/protobuf/any.proto")
    protoBuilder.addService(service)
    messages
      .filterNot { desc =>
        desc.getName == JavaPbAny.getDescriptor.getFullName
      }
      .foreach(protoBuilder.addMessageType)

    // finally build all final descriptor
    Descriptors.FileDescriptor.buildFrom(protoBuilder.build, dependencies)
  }
}
