/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

import com.google.protobuf.Descriptors

object TestProtoMessageTypeExtractor extends ModelBuilder.ProtoMessageTypeExtractor {
  override def apply(descriptor: Descriptors.GenericDescriptor): ProtoMessageType =
    ProtoMessageType(
      descriptor.getName,
      descriptor.getName,
      packageName(descriptor),
      Some(fileDescriptorObject(descriptor)))

  override def fileDescriptorObject(descriptor: Descriptors.GenericDescriptor): ProtoMessageType = {
    val parent = packageName(descriptor)
    ProtoMessageType(
      parent.javaOuterClassname,
      parent.javaOuterClassname,
      parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
      None)
  }

  override def packageName(descriptor: Descriptors.GenericDescriptor): PackageNaming = {
    val fileDescriptor = descriptor.getFile
    if (fileDescriptor.getName == s"google.protobuf.${descriptor.getName}.placeholder.proto") {
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

}
