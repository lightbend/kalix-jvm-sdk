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

package com.lightbend.akkasls.codegen

import scala.jdk.CollectionConverters._

import com.google.protobuf.Descriptors

/**
 * A fully qualified name that can be resolved in any target language
 *
 * @param descriptorObject
 *   code representation of the descriptor that defined this type, if any
 */
case class FullyQualifiedName(
    protoName: String,
    name: String,
    parent: PackageNaming,
    descriptorObject: Option[FullyQualifiedName]) {

  lazy val fullQualifiedName = s"${parent.javaPackage}.$name"
  lazy val fullName = {
    if (parent.javaMultipleFiles) name
    else s"${parent.javaOuterClassname}.$name"
  }

  lazy val typeImport = s"${parent.javaPackage}.$fullName"
  lazy val descriptorImport = descriptorObject.get

  /**
   * 'base' name of the file that will contain this fqn relative to the package root,
   * i.e. 'com/example/MyService'
   */
  def fileBasename =
    parent.javaPackage.replace('.', '/') + "/" + name

}

object FullyQualifiedName {

  /**
   * Creates a FullyQualifiedName without a descriptor.
   */
  def noDescriptor(name: String, parent: PackageNaming) =
    FullyQualifiedName(name, name, parent, None)
}

case class PackageNaming(
    protoFileName: String,
    name: String,
    protoPackage: String,
    javaPackageOption: Option[String],
    javaOuterClassnameOption: Option[String],
    javaMultipleFiles: Boolean) {
  lazy val javaPackage: String = javaPackageOption.getOrElse(protoPackage)
  def scalaPackage: String = javaPackage
  def javaOuterClassname: String = javaOuterClassnameOption.getOrElse(name)
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

    val generalOptions = descriptor.getOptions.getAllFields.asScala.map { case (fieldDescriptor, field) =>
      (fieldDescriptor.getFullName, field)
    }

    val javaPackage =
      generalOptions.get("google.protobuf.FileOptions.java_package").map(_.toString())

    val javaOuterClassnameOption =
      generalOptions
        .get("google.protobuf.FileOptions.java_outer_classname")
        .map(_.toString())

    val javaOuterClassname =
      javaOuterClassnameOption.getOrElse {
        val existingNames =
          descriptor.getMessageTypes.asScala.map(_.getName) ++
          descriptor.getEnumTypes.asScala.map(_.getName) ++
          descriptor.getServices.asScala.map(_.getName)
        if (existingNames.contains(name)) name + "OuterClass" else name
      }

    val javaMultipleFiles =
      generalOptions.get("google.protobuf.FileOptions.java_multiple_files").contains(true)

    PackageNaming(
      descriptor.getName,
      name,
      descriptor.getPackage,
      javaPackage,
      Some(javaOuterClassname),
      javaMultipleFiles)
  }
}
