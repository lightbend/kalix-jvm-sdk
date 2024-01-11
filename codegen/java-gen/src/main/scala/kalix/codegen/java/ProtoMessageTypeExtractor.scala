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
import kalix.codegen.{ ModelBuilder, PackageNaming, ProtoMessageType }

object ProtoMessageTypeExtractor extends ModelBuilder.ProtoMessageTypeExtractor {
  override def apply(descriptor: Descriptors.GenericDescriptor): ProtoMessageType = {
    val pack = packageName(descriptor)
    ProtoMessageType(descriptor.getName, descriptor.getName, pack, Some(fileDescriptorObject(descriptor)))
  }

  override def packageName(descriptor: Descriptors.GenericDescriptor): PackageNaming = {
    val fileDescriptor = descriptor.getFile
    if (fileDescriptor.getName.startsWith("google.protobuf") && descriptor.getName.endsWith("Value")) {
      PackageNaming(
        descriptor.getName,
        descriptor.getName,
        fileDescriptor.getPackage,
        // Single shared file descriptor for the NnnValue wrapper types in Java
        Some("com.google.protobuf"),
        Some("WrappersProto"),
        javaMultipleFiles = true)
    } else if (fileDescriptor.getName == s"google.protobuf.${descriptor.getName}.placeholder.proto") {
      // In the case of placeholders for standard google types, we need to assume the package naming
      // These defaults are based on the protos from https://github.com/protocolbuffers/protobuf/blob/master/src/google/protobuf
      PackageNaming(
        descriptor.getName,
        descriptor.getName,
        fileDescriptor.getPackage,
        Some(s"com.${fileDescriptor.getPackage}"),
        Some(s"${descriptor.getName}Proto"),
        javaMultipleFiles = true)
    } else PackageNaming.from(fileDescriptor)
  }

  override def fileDescriptorObject(descriptor: Descriptors.GenericDescriptor): ProtoMessageType = {
    val parent = packageName(descriptor)
    ProtoMessageType(
      parent.javaOuterClassname,
      parent.javaOuterClassname,
      parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
      None)
  }
}
