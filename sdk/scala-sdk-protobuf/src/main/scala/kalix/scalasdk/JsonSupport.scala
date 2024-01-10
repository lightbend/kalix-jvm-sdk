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

package kalix.scalasdk

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.{ JsonSupport => JavaJsonSupport, Kalix }
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.jdk.OptionConverters._
import scala.reflect.ClassTag

object JsonSupport {

  getObjectMapper().registerModule(new DefaultScalaModule)

  /**
   * The Jackson ObjectMapper that is used for encoding and decoding JSON. You may adjust it's configuration, but that
   * must only be performed before starting [[Kalix]]
   */
  def getObjectMapper(): ObjectMapper = JavaJsonSupport.getObjectMapper

  /**
   * Encode the given value as JSON using Jackson and put the encoded string as bytes in a protobuf `Any` with the type
   * URL `json.kalix.io/[valueClassName]`.
   *
   * <p>Note that if the serialized Any is published to a pub/sub topic that is consumed by an external service using
   * the class name suffix this introduces coupling as the internal class name of this service becomes known to the
   * outside of the service (and for example renaming it may break existing consumers). For such cases consider using
   * the overload with an explicit name for the JSON type instead.
   *
   * @see
   *   [[encodeJson(T, java.lang.String)]]
   */
  def encodeJson[T](value: T): ScalaPbAny = ScalaPbAny.fromJavaProto(JavaJsonSupport.encodeJson(value))

  /**
   * Encode the given value as JSON using Jackson and put the encoded string as bytes in a protobuf Any with the type
   * URL `json.kalix.io/[jsonType]`.
   *
   * @param value
   *   the object to encode as JSON, must be an instance of a class properly annotated with the needed Jackson
   *   annotations.
   * @param jsonType
   *   A discriminator making it possible to identify which type of object is in the JSON, useful for example when
   *   multiple different objects are passed through a pub/sub topic.
   * @throws IllegalArgumentException
   *   if the given value cannot be turned into JSON
   */
  def encodeJson[T](value: T, jsonType: String): ScalaPbAny =
    ScalaPbAny.fromJavaProto(JavaJsonSupport.encodeJson(value, jsonType))

  /**
   * Decode the given protobuf `Any` object to an instance of `T` using Jackson. The object must have the JSON string as
   * bytes as value and a type URL starting with `json.kalix.io/`.
   *
   * @param valueClass
   *   The type of class to deserialize the object to, the class must have the proper Jackson annotations for
   *   deserialization.
   * @return
   *   The decoded object
   * @throws IllegalArgumentException
   *   if the given value cannot be decoded to a T
   */
  def decodeJson[T: ClassTag](any: ScalaPbAny): T =
    JavaJsonSupport.decodeJson(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], ScalaPbAny.toJavaProto(any))

  /**
   * Decode the given protobuf Any to an instance of `T`` using Jackson but only if the suffix of the type URL matches
   * the given jsonType.
   *
   * @return
   *   An Option containing the successfully decoded value or None if the type suffix does not match.
   * @throws IllegalArgumentException
   *   if the suffix matches but the Any cannot be parsed into a T
   */
  def decodeJson[T: ClassTag](jsonType: String, any: ScalaPbAny): Option[T] =
    JavaJsonSupport
      .decodeJson(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], jsonType, ScalaPbAny.toJavaProto(any))
      .toScala

}
