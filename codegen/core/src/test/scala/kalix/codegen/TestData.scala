/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

object TestData {
  private val javaStylePackageNamingTemplate: PackageNaming =
    PackageNaming(
      "undefined.proto",
      "Undefined",
      "undefined",
      None,
      Some("UndefinedOuterClass"),
      javaMultipleFiles = false)

  def apply(packageNamingTemplate: PackageNaming): TestData = {
    new TestData(packageNamingTemplate)
  }

  val javaStyle: TestData = apply(javaStylePackageNamingTemplate)
  val scalaStyle: TestData = apply(javaStylePackageNamingTemplate.copy(javaOuterClassnameOption = None))

  def guessDescriptor(protoName: String, proto: PackageNaming): Option[ProtoMessageType] =
    proto.javaOuterClassnameOption match {
      case Some(outer) =>
        Some(
          ProtoMessageType(outer, outer, proto.copy(javaOuterClassnameOption = None, javaMultipleFiles = true), None))
      case None =>
        Some(
          ProtoMessageType(
            protoName + "Proto",
            protoName + "Proto",
            proto.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
            None))
    }

  def protoMessageType(parent: PackageNaming, name: String): ProtoMessageType = {
    ProtoMessageType(
      name,
      name,
      parent,
      parent.javaOuterClassnameOption match {
        case Some(outer) =>
          Some(
            ProtoMessageType(
              outer,
              outer,
              parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
              None))
        case None =>
          def capitalize(s: String, capitalizeNext: Boolean = true): String =
            s.headOption match {
              case None      => ""
              case Some('_') => capitalize(s.tail, true)
              case Some(c) =>
                if (capitalizeNext) c.toUpper + capitalize(s.tail, false)
                else c + capitalize(s.tail, false)
            }
          val protoClassName = capitalize(parent.protoFileName.replaceAll(".proto", "") + "Proto")
          Some(
            ProtoMessageType(
              protoClassName,
              protoClassName,
              parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
              None))
      })
  }
}

/**
 * Used by java and scala codegen projects for their tests
 */
class TestData(val packageNamingTemplate: PackageNaming) {

  def domainProto(suffix: String = ""): PackageNaming =
    packageNamingTemplate.copy(
      "domain.proto",
      s"Domain$suffix",
      "com.example.service.domain",
      javaOuterClassnameOption = packageNamingTemplate.javaOuterClassnameOption.map(_ => s"EntityOuterClass$suffix"))

}
