/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import scala.jdk.CollectionConverters._
import com.google.protobuf.Descriptors

/**
  * A fully qualified name that can be resolved in any target language
  */
case class FullyQualifiedName(
    name: String,
    parent: PackageNaming
) {
  lazy val fullName = s"${parent.pkg}.$name"
}

object FullyQualifiedName {
  def from(descriptor: Descriptors.GenericDescriptor): FullyQualifiedName = {
    val fileDescriptor = descriptor.getFile

    val packageNaming =
      if (fileDescriptor.getName == s"google.protobuf.${descriptor.getName}.placeholder.proto") {
        // In the case of placeholders for standard google types, we need to assume the package naming
        // These defaults are based on the protos from https://github.com/protocolbuffers/protobuf/blob/master/src/google/protobuf
        PackageNaming(
          descriptor.getName,
          fileDescriptor.getPackage,
          Some(s"google.golang.org/protobuf/types/known/${descriptor.getName.toLowerCase}pb"),
          Some(s"com.${fileDescriptor.getPackage}"),
          Some(s"${descriptor.getName}Proto"),
          javaMultipleFiles = true
        )
      } else PackageNaming.from(fileDescriptor)
    FullyQualifiedName(
      descriptor.getName,
      packageNaming
    )
  }
}

/**
  * The details of a package's naming, sufficient to construct fully qualified names in any target language
  */
case class PackageNaming(
    name: String,
    pkg: String,
    goPackage: Option[String],
    javaPackageOption: Option[String],
    javaOuterClassnameOption: Option[String],
    javaMultipleFiles: Boolean
) {
  lazy val javaPackage: String        = javaPackageOption.getOrElse(pkg)
  lazy val javaOuterClassname: String = javaOuterClassnameOption.getOrElse(name)
}

object PackageNaming {
  def from(descriptor: Descriptors.FileDescriptor): PackageNaming = {
    val name =
      descriptor.getName
        .split('/')
        .last
        .reverse
        .dropWhile(_ != '.')
        .tail
        .reverse
        .split('_')
        .map(s => s.headOption.fold("")(first => s"${first.toUpper.toString}${s.tail}"))
        .mkString

    val generalOptions = descriptor.getOptions.getAllFields.asScala.map {
      case (fieldDescriptor, field) => (fieldDescriptor.getFullName, field)
    }

    val goPackage = generalOptions.get("google.protobuf.FileOptions.go_package").map(_.toString())
    val javaPackage =
      generalOptions.get("google.protobuf.FileOptions.java_package").map(_.toString())

    val javaOuterClassname =
      generalOptions
        .get("google.protobuf.FileOptions.java_outer_classname")
        .map(_.toString())

    val javaMultipleFiles =
      generalOptions.get("google.protobuf.FileOptions.java_multiple_files").contains(true)

    PackageNaming(
      name,
      descriptor.getPackage,
      goPackage,
      javaPackage,
      javaOuterClassname,
      javaMultipleFiles
    )
  }
}
