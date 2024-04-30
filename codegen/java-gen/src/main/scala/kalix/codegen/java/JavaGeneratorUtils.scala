/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.Imports
import kalix.codegen.MessageType
import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ProtoMessageType
import kalix.codegen.SourceGeneratorUtils.CodeBlock
import kalix.codegen.SourceGeneratorUtils.typeImport

object JavaGeneratorUtils {
  def typeName(messageType: MessageType)(implicit imports: Imports): String = {
    val directParent =
      messageType match {
        case proto: ProtoMessageType if !proto.parent.javaMultipleFiles =>
          s"${messageType.packageName}.${proto.parent.javaOuterClassname}"
        case _ => messageType.packageName
      }

    if (messageType.packageName.isEmpty) messageType.name
    else if (imports.contains(messageType.fullyQualifiedName)) messageType.name
    else if (imports.currentPackage == directParent) messageType.name
    else if (imports.contains(directParent) || imports.currentPackage == messageType.packageName)
      directParent.split("\\.").last + "." + messageType.name
    else directParent + s".${messageType.name}"
  }

  def writeImports(imports: Imports): String =
    imports.ordered
      .map(_.map { imported => s"import $imported;" }.mkString("\n"))
      .mkString("\n\n")

  def dataType(typeArgument: ModelBuilder.TypeArgument): String =
    typeArgument match {
      case ModelBuilder.MessageTypeArgument(messageType) =>
        messageType.fullName
      case ModelBuilder.ScalarTypeArgument(scalar) =>
        scalar match {
          case ModelBuilder.ScalarType.Int32 | ModelBuilder.ScalarType.UInt32 | ModelBuilder.ScalarType.SInt32 |
              ModelBuilder.ScalarType.Fixed32 | ModelBuilder.ScalarType.SFixed32 =>
            "Integer"
          case ModelBuilder.ScalarType.Int64 | ModelBuilder.ScalarType.UInt64 | ModelBuilder.ScalarType.SInt64 |
              ModelBuilder.ScalarType.Fixed64 | ModelBuilder.ScalarType.SFixed64 =>
            "Long"
          case ModelBuilder.ScalarType.Double => "Double"
          case ModelBuilder.ScalarType.Float  => "Float"
          case ModelBuilder.ScalarType.Bool   => "Boolean"
          case ModelBuilder.ScalarType.String => "String"
          case ModelBuilder.ScalarType.Bytes  => "ByteString"
          case _                              => "?"
        }
    }

  def parameterizeDataType(replicatedData: ModelBuilder.ReplicatedData): String = {
    val typeArguments =
      replicatedData match {
        // special case ReplicatedMap as heterogeneous with ReplicatedData values
        case ModelBuilder.ReplicatedMap(key) => Seq(dataType(key), "ReplicatedData")
        case data                            => data.typeArguments.map(typ => dataType(typ))
      }
    parameterizeTypes(typeArguments)
  }

  def parameterizeTypes(types: Iterable[String]): String =
    if (types.isEmpty) ""
    else types.mkString("<", ", ", ">")

  def generate(parent: PackageNaming, block: CodeBlock, packageImports: Seq[PackageNaming] = Nil): String = {
    val packageImportStrings = packageImports.map(_.javaPackage)
    implicit val imports = new Imports(
      parent.javaPackage,
      packageImportStrings ++ block.messageTypes
        .filter {
          case proto: ProtoMessageType => proto.parent.javaPackage.nonEmpty
          case _                       => true
        }
        .filterNot { messageType => packageImportStrings.contains(messageType.packageName) }
        .map(typeImport))

    s"""package ${parent.javaPackage};
       |
       |${writeImports(imports)}
       |
       |${block.write(imports, typeName(_))}
       |""".stripMargin.replaceAll("[ \t]+\n", "\n")
  }
}
