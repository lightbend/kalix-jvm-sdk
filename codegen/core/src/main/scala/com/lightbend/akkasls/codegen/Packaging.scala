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

  /**
   * The fully qualified type name of the proto type. This should rarely be used during codegen, since language-specific
   * changes might have to be applied, and imports taken into account. In that case use the language-specific `typeName`
   * utility function instead.
   */
  lazy val fullyQualifiedProtoName = s"${parent.protoPackage}.$name"

  /**
   * The fully qualified type name as seen in generated Java code.
   */
  lazy val fullyQualifiedJavaName = s"${parent.javaPackage}.$name"

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

  /**
   * Create a 'derived' name based on a name, such as 'FooProvider' based on 'Foo'.
   *
   * Notably also removes any outer class name from the parent, since 'derived' classes are always outside of the outer
   * class.
   */
  def deriveName(derive: String => String): FullyQualifiedName =
    copy(name = derive(name), parent = parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true))
}

object FullyQualifiedName {

  /**
   * Creates a FullyQualifiedName without a descriptor.
   */
  def noDescriptor(name: String, parent: PackageNaming) =
    FullyQualifiedName(name, name, parent, None)

  /**
   * Creates a FullyQualifiedName without a descriptor.
   */
  def noDescriptor(name: String, parent: String) =
    FullyQualifiedName(name, name, PackageNaming.noDescriptor(parent), None)
}

case class PackageNaming(
    protoFileName: String,
    name: String,
    protoPackage: String,
    javaPackageOption: Option[String] = None,
    javaOuterClassnameOption: Option[String] = None,
    javaMultipleFiles: Boolean = false) {
  lazy val javaPackage: String = javaPackageOption.getOrElse(protoPackage)
  def scalaPackage: String = javaPackage
  def javaOuterClassname: String = javaOuterClassnameOption.getOrElse(name)
}

object PackageNaming {

  /**
   * Package for classes that aren't derived from a .proto
   */
  def noDescriptor(parent: String): PackageNaming =
    PackageNaming("", "", "", Some(parent), None, javaMultipleFiles = true)

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
