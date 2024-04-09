/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
