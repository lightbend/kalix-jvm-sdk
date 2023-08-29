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

package kalix.javasdk.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import scala.jdk.CollectionConverters._

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.annotations.Migration
import kalix.javasdk.annotations.TypeName
import kalix.javasdk.impl.AnySupport.BytesPrimitive
import org.slf4j.LoggerFactory

private[kalix] class JsonMessageCodec extends MessageCodec {

  private val log = LoggerFactory.getLogger(getClass)
  private[kalix] case class TypeHint(currenTypeHintWithVersion: String, allTypeHints: List[String])

  private val typeHints: ConcurrentMap[Class[_], TypeHint] = new ConcurrentHashMap()
  private[kalix] val reversedTypeHints: ConcurrentMap[String, Class[_]] = new ConcurrentHashMap()

  /**
   * In the Java SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): ScalaPbAny = {
    if (value == null) throw NullSerializationException
    value match {
      case javaPbAny: JavaPbAny   => ScalaPbAny.fromJavaProto(javaPbAny)
      case scalaPbAny: ScalaPbAny => scalaPbAny
      case bytes: Array[Byte]     => ScalaPbAny.fromJavaProto(JavaPbAny.pack(BytesValue.of(ByteString.copyFrom(bytes))))
      case other => ScalaPbAny.fromJavaProto(JsonSupport.encodeJson(other, lookupTypeHintWithVersion(other)))
    }
  }

  override def encodeJava(value: Any): JavaPbAny = {
    if (value == null) throw NullSerializationException
    value match {
      case javaPbAny: JavaPbAny   => javaPbAny
      case scalaPbAny: ScalaPbAny => ScalaPbAny.toJavaProto(scalaPbAny)
      case other                  => JsonSupport.encodeJson(other, lookupTypeHintWithVersion(other))
    }
  }

  private def lookupTypeHintWithVersion(value: Any): String =
    lookupTypeHint(value.getClass).currenTypeHintWithVersion

  private[kalix] def lookupTypeHint(clz: Class[_]): TypeHint = {
    typeHints.computeIfAbsent(clz, computeTypeHint)
  }

  private[kalix] def registerTypeHints(clz: Class[_]) = {
    lookupTypeHint(clz)
    if (clz.getAnnotation(classOf[JsonSubTypes]) != null) {
      //registering all subtypes
      clz
        .getAnnotation(classOf[JsonSubTypes])
        .value()
        .map(_.value())
        .foreach(lookupTypeHint)
    }
  }

  private def computeTypeHint(clz: Class[_]): TypeHint = {
    val typeName = Option(clz.getAnnotation(classOf[TypeName]))
      .collect { case ann if ann.value().trim.nonEmpty => ann.value() }
      .getOrElse(clz.getName)

    val (version, supportedClassNames) = getVersionAndSupportedClassNames(clz)
    val typeNameWithVersion = typeName + (if (version == 0) "" else "#" + version)

    //TODO verify if this could be replaced by sth smarter/safer
    addToReversedCache(clz, typeName)
    supportedClassNames.foreach(className => addToReversedCache(clz, className))

    TypeHint(typeNameWithVersion, typeName :: supportedClassNames)
  }

  private def addToReversedCache(clz: Class[_], typeName: String) = {
    reversedTypeHints.compute(
      typeName,
      (_, currentValue) => {
        if (currentValue == null) {
          clz
        } else if (currentValue == clz) {
          currentValue
        } else {
          throw new IllegalStateException(
            "Collision with existing existing mapping " + currentValue + " -> " + typeName + ". The same type name can't be used for other class " + clz)
        }
      })
  }

  private def getVersionAndSupportedClassNames(clz: Class[_]): (Int, List[String]) = {
    Option(clz.getAnnotation(classOf[Migration]))
      .map(_.value())
      .map(migrationClass => migrationClass.getConstructor().newInstance())
      .map(migration =>
        (migration.currentVersion(), migration.supportedClassNames().asScala.toList)) //TODO what about TypeName
      .getOrElse((0, List.empty))
  }

  def typeUrlFor(clz: Class[_]): String = {
    if (clz == classOf[Array[Byte]]) {
      BytesPrimitive.fullName
    } else {
      JsonSupport.KALIX_JSON + lookupTypeHint(clz).currenTypeHintWithVersion
    }
  }

  def typeUrlsFor(clz: Class[_]): List[String] = {
    if (clz == classOf[Array[Byte]]) {
      List(BytesPrimitive.fullName)
    } else {
      lookupTypeHint(clz).allTypeHints.map(JsonSupport.KALIX_JSON + _)
    }
  }

  override def decodeMessage(value: ScalaPbAny): Any = {
    value
  }

  private[kalix] def removeVersion(typeName: String) = {
    typeName.split("#").head
  }
}

/**
 * Used in workflows where it is necessary to decode message directly to Java class for calls and transitions. This
 * behavior is not correct for other components (Action, Views) where e.g. subscription can't decode the payload to Java
 * class too early (typeUrl is used for the component logic). It must reuse the same cache as JsonMessageCodec.
 */
private[kalix] class StrictJsonMessageCodec(delegate: JsonMessageCodec) extends MessageCodec {

  override def decodeMessage(value: ScalaPbAny): Any =
    if (value.typeUrl.startsWith(JsonSupport.KALIX_JSON)) {
      val any = ScalaPbAny.toJavaProto(value)
      val typeName = delegate.removeVersion(value.typeUrl.replace(JsonSupport.KALIX_JSON, ""))
      val typeClass = delegate.reversedTypeHints.get(typeName)
      if (typeClass == null) {
        throw new IllegalStateException(s"Cannot decode ${value.typeUrl} message type. Class mapping not found.")
      } else {
        JsonSupport.decodeJson(typeClass, any)
      }
    } else {
      value
    }

  override def encodeScala(value: Any): ScalaPbAny =
    delegate.encodeScala(value)

  override def encodeJava(value: Any): JavaPbAny =
    delegate.encodeJava(value)

  override def typeUrlFor(clz: Class[_]): String = delegate.typeUrlFor(clz)
}
