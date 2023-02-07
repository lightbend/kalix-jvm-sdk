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

package kalix.springsdk.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.NullSerializationException
import kalix.springsdk.annotations.TypeName

private[springsdk] class SpringSdkMessageCodec extends MessageCodec {

  private val cache: ConcurrentMap[Class[_], String] = new ConcurrentHashMap()
  private[springsdk] val reversedCache: ConcurrentMap[String, Class[_]] = new ConcurrentHashMap()

  /**
   * In the Spring SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): ScalaPbAny = {
    if (value == null) throw NullSerializationException
    value match {
      case javaPbAny: JavaPbAny   => ScalaPbAny.fromJavaProto(javaPbAny)
      case scalaPbAny: ScalaPbAny => scalaPbAny
      case other                  => ScalaPbAny.fromJavaProto(JsonSupport.encodeJson(other, lookupTypeHint(other)))
    }
  }

  override def encodeJava(value: Any): JavaPbAny = {
    if (value == null) throw NullSerializationException
    value match {
      case javaPbAny: JavaPbAny   => javaPbAny
      case scalaPbAny: ScalaPbAny => ScalaPbAny.toJavaProto(scalaPbAny)
      case other                  => JsonSupport.encodeJson(other, lookupTypeHint(other))
    }
  }

  private def lookupTypeHint(value: Any): String =
    lookupTypeHint(value.getClass)

  private[kalix] def lookupTypeHint(clz: Class[_]): String = {
    val typeName = Option(clz.getAnnotation(classOf[TypeName]))
      .collect { case ann if ann.value().trim.nonEmpty => ann.value() }
      .getOrElse(clz.getSimpleName) //TODO getName to minimize collision chance, is it backward compatible
    cache.computeIfAbsent(clz, _ => typeName)
    //TODO verify if this could be replaced by sth smarter/safer
    reversedCache.compute(
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

    typeName
  }

  def typeUrlFor(clz: Class[_]) =
    JsonSupport.KALIX_JSON + lookupTypeHint(clz)

  override def decodeMessage(value: ScalaPbAny): Any = {
    value
  }
}

/**
 * Used in workflows where it is necessary to decode message directly to Java class for calls and transitions. This
 * behavior is not correct for other components (Action, Views) where e.g. subscription can't decode the payload to Java
 * class too early (typeUrl is used for the component logic). It must reuse the same cache as SpringSdkMessageCodec.
 */
private[springsdk] class SpringSdkMessageCodecJson(delegate: SpringSdkMessageCodec) extends MessageCodec {

  override def decodeMessage(value: ScalaPbAny): Any =
    if (value.typeUrl.startsWith(JsonSupport.KALIX_JSON)) {
      val any = ScalaPbAny.toJavaProto(value)
      val typeName = value.typeUrl.replace(JsonSupport.KALIX_JSON, "")
      val typeClass = delegate.reversedCache.get(typeName)
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
}
