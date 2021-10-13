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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen.{ FullyQualifiedName, Imports, ModelBuilder }

object JavaGeneratorUtils {
  def typeName(fqn: FullyQualifiedName)(implicit imports: Imports): String = {
    if (imports.contains(fqn.fullQualifiedName)) fqn.name
    else if (fqn.parent.javaPackage == imports.currentPackage) fqn.name
    else if (imports.contains(fqn.parent.javaPackage))
      fqn.parent.javaPackage.split("\\.").last + "." + fqn.name
    else fqn.fullQualifiedName
  }

  def writeImports(imports: Imports): String = {
    imports.imports
      .map { imported => s"import $imported;" }
      .mkString("\n")
  }

  def dataType(typeArgument: ModelBuilder.TypeArgument)(implicit imports: Imports): String =
    typeArgument match {
      case ModelBuilder.MessageTypeArgument(fqn) =>
        fqn.fullName
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
    else types.mkString("<", ", ", ">")

}
