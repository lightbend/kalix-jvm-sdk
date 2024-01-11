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

package kalix.javasdk.impl.http

import java.lang.{ Boolean => JBoolean }
import java.lang.{ Double => JDouble }
import java.lang.{ Float => JFloat }
import java.lang.{ Integer => JInteger }
import java.lang.{ Long => JLong }
import java.lang.{ Short => JShort }

import akka.parboiled2.util.Base64
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.{ ByteString => ProtobufByteString }

/**
 * INTERNAL API
 *
 * The actually running/request handling endpoint, one HTTP endpoint serves/handles
 */
private object HttpEndpointMethod {

  final val ParseShort: String => Option[JShort] =
    s =>
      try Option(JShort.valueOf(s))
      catch { case _: NumberFormatException => None }

  final val ParseInt: String => Option[JInteger] =
    s =>
      try Option(JInteger.valueOf(s))
      catch { case _: NumberFormatException => None }

  final val ParseLong: String => Option[JLong] =
    s =>
      try Option(JLong.valueOf(s))
      catch { case _: NumberFormatException => None }

  final val ParseFloat: String => Option[JFloat] =
    s =>
      try Option(JFloat.valueOf(s))
      catch { case _: NumberFormatException => None }

  final val ParseDouble: String => Option[JDouble] =
    s =>
      try Option(JDouble.valueOf(s))
      catch { case _: NumberFormatException => None }

  final val ParseString: String => Option[String] =
    s => Option(s)

  private[this] final val someJTrue = Some(JBoolean.TRUE)
  private[this] final val someJFalse = Some(JBoolean.FALSE)

  final val ParseBoolean: String => Option[JBoolean] =
    _.toLowerCase match {
      case "true"  => someJTrue
      case "false" => someJFalse
      case _       => None
    }

// Reads a rfc2045 encoded Base64 string
  final val ParseBytes: String => Option[ProtobufByteString] =
    s => Some(ProtobufByteString.copyFrom(Base64.rfc2045.decode(s))) // Make cheaper? Protobuf has a Base64 decoder?

  final def suitableParserFor(field: FieldDescriptor)(whenIllegal: String => Nothing): String => Option[Any] =
    field.getJavaType match {
      case JavaType.BOOLEAN     => ParseBoolean
      case JavaType.BYTE_STRING => ParseBytes
      case JavaType.DOUBLE      => ParseDouble
      case JavaType.ENUM        => whenIllegal("Enum path parameters not supported!")
      case JavaType.FLOAT       => ParseFloat
      case JavaType.INT         => ParseInt
      case JavaType.LONG        => ParseLong
      case JavaType.MESSAGE     => whenIllegal("Message path parameters not supported!")
      case JavaType.STRING      => ParseString
    }

}
