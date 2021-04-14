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
  def fromDescriptor(descriptor: Descriptors.Descriptor) =
    FullyQualifiedName(
      descriptor.getName(),
      PackageNaming.fromFileDescriptor(descriptor.getFile())
    )
}

/**
  * The details of a package's naming, sufficient to construct fully qualified names in any target language
  */
case class PackageNaming(
    pkg: String,
    goPackage: Option[String],
    javaPackageOption: Option[String],
    javaOuterClassname: Option[String]
) {
  lazy val javaPackage = javaPackageOption.getOrElse(pkg)
}

object PackageNaming {
  def fromFileDescriptor(descriptor: Descriptors.FileDescriptor) = {
    val generalOptions = descriptor.getOptions.getAllFields.asScala
    val goPackage = generalOptions
      .find(_._1.getFullName == "google.protobuf.FileOptions.go_package")
      .map(_._2.toString)
    val javaPackage = generalOptions
      .find(_._1.getFullName == "google.protobuf.FileOptions.java_package")
      .map(_._2.toString)
    val javaOuterClassname = generalOptions
      .find(_._1.getFullName == "google.protobuf.FileOptions.java_outer_classname")
      .map(_._2.toString)

    PackageNaming(
      descriptor.getPackage(),
      goPackage,
      javaPackage,
      javaOuterClassname
    )
  }
}
