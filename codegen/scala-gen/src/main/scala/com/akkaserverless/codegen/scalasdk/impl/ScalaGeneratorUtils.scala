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

package com.akkaserverless.codegen.scalasdk.impl

import com.lightbend.akkasls.codegen.File
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.MessageType
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PackageNaming
import com.lightbend.akkasls.codegen.PojoMessageType
import com.lightbend.akkasls.codegen.ProtoMessageType
import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

object ScalaGeneratorUtils {

  import Types._
  import Types.ReplicatedEntity._

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

  def generateSerializers(pojoMessageTypes: Iterable[PojoMessageType]): CodeBlock =
    if (pojoMessageTypes.isEmpty) {
      c"$Serializer.noopSerializer"
    } else {
      c"""$Serializers
          |  ${pojoMessageTypes.map(typ => c".add(new $JsonSerializer(classOf[${typ.name}]))")}
          """
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

  def dataTypeCodeBlock(typeArgument: ModelBuilder.TypeArgument): CodeBlock =
    typeArgument match {
      case ModelBuilder.MessageTypeArgument(messageType) =>
        c"$messageType"
      case ModelBuilder.ScalarTypeArgument(scalar) =>
        scalar match {
          case ModelBuilder.ScalarType.Int32 | ModelBuilder.ScalarType.UInt32 | ModelBuilder.ScalarType.SInt32 |
              ModelBuilder.ScalarType.Fixed32 | ModelBuilder.ScalarType.SFixed32 =>
            c"Int"
          case ModelBuilder.ScalarType.Int64 | ModelBuilder.ScalarType.UInt64 | ModelBuilder.ScalarType.SInt64 |
              ModelBuilder.ScalarType.Fixed64 | ModelBuilder.ScalarType.SFixed64 =>
            c"Long"
          case ModelBuilder.ScalarType.Double => c"Double"
          case ModelBuilder.ScalarType.Float  => c"Float"
          case ModelBuilder.ScalarType.Bool   => c"Boolean"
          case ModelBuilder.ScalarType.String => c"String"
          case ModelBuilder.ScalarType.Bytes  => c"$ByteString"
          case _                              => c"_"
        }
    }

  def parameterizeDataTypeCodeBlock(replicatedData: ModelBuilder.ReplicatedData): CodeBlock =
    replicatedData match {
      // special case ReplicatedMap as heterogeneous with ReplicatedData values
      case ModelBuilder.ReplicatedMap(key) => c"[${dataTypeCodeBlock(key)}, $ReplicatedData]"
      case data =>
        data.typeArguments.toList.map(typ => dataTypeCodeBlock(typ)) match {
          case first :: second :: Nil => c"[$first, $second]"
          case single :: Nil          => c"[$single]"
          case Nil                    => c""
          case _                      =>
            // this won't happen. All supported replicated data types have either zero, one or two type params
            // this is here to make the compiler happy and to help us find it in case we add a new replicated data type
            throw new IllegalArgumentException("Found more than two type parameter")
        }
    }

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
