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

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Imports
import kalix.codegen.MessageType
import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ClassMessageType
import kalix.codegen.ProtoMessageType
import kalix.codegen.SourceGeneratorUtils._

object ScalaGeneratorUtils {
  def typeName(messageType: MessageType)(implicit imports: Imports): String =
    if (messageType.fullyQualifiedName == "com.google.protobuf.any.Any") "ScalaPbAny"
    else if (imports.contains(messageType.fullyQualifiedName)) messageType.name
    else if (messageType.packageName == imports.currentPackage) messageType.name
    else if (imports.contains(messageType.packageName))
      messageType.packageName.split("\\.").last + "." + messageType.name
    else
      s"${messageType.packageName}.${messageType.name}"

  def writeImports(imports: Imports): String = {
    imports.ordered
      .map(_.map { imported =>
        if (imported == "com.google.protobuf.any.Any") {
          s"import com.google.protobuf.any.{ Any => ScalaPbAny }"
        } else
          s"import $imported"
      }.mkString("\n"))
      .mkString("\n\n")
  }

  def dataType(typeArgument: ModelBuilder.TypeArgument)(implicit imports: Imports): String =
    typeArgument match {
      case ModelBuilder.MessageTypeArgument(messageType) =>
        typeName(messageType)
      case ModelBuilder.ScalarTypeArgument(scalar) =>
        scalar match {
          case ModelBuilder.ScalarType.Int32 | ModelBuilder.ScalarType.UInt32 | ModelBuilder.ScalarType.SInt32 |
              ModelBuilder.ScalarType.Fixed32 | ModelBuilder.ScalarType.SFixed32 =>
            "Int"
          case ModelBuilder.ScalarType.Int64 | ModelBuilder.ScalarType.UInt64 | ModelBuilder.ScalarType.SInt64 |
              ModelBuilder.ScalarType.Fixed64 | ModelBuilder.ScalarType.SFixed64 =>
            "Long"
          case ModelBuilder.ScalarType.Double => "Double"
          case ModelBuilder.ScalarType.Float  => "Float"
          case ModelBuilder.ScalarType.Bool   => "Boolean"
          case ModelBuilder.ScalarType.String => "String"
          case ModelBuilder.ScalarType.Bytes  => "ByteString"
          case _                              => "_"
        }
    }

  def parameterizeDataType(replicatedData: ModelBuilder.ReplicatedData)(implicit imports: Imports): String = {
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
    else types.mkString("[", ", ", "]")

  def generate(
      parent: PackageNaming,
      name: String,
      block: CodeBlock,
      packageImports: Seq[PackageNaming] = Nil): File = {
    val packageImportStrings = packageImports.map(_.scalaPackage)
    implicit val imports = new Imports(
      parent.scalaPackage,
      packageImportStrings ++ block.messageTypes
        .filter {
          case proto: ProtoMessageType => proto.parent.javaPackage.nonEmpty
          case _                       => true
        }
        .filterNot { messageType => packageImportStrings.contains(messageType.packageName) }
        .map(typeImport))

    File.scala(
      parent.scalaPackage,
      name,
      s"""package ${parent.scalaPackage}
         |
         |${writeImports(imports)}
         |
         |${block.write(imports, typeName(_))}
         |""".stripMargin.replaceAll("[ \t]+\n", "\n"))
  }
}
