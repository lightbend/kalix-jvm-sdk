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
import kalix.springsdk.annotations.TypeName

private[springsdk] class SpringSdkMessageCodec extends MessageCodec {

  private val cache: ConcurrentMap[Class[_], String] = new ConcurrentHashMap()
  private val reversedCache: ConcurrentMap[String, Class[_]] = new ConcurrentHashMap()

  /**
   * In the Spring SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): ScalaPbAny = {
    value match {
      case javaPbAny: JavaPbAny   => ScalaPbAny.fromJavaProto(javaPbAny)
      case scalaPbAny: ScalaPbAny => scalaPbAny
      case other                  => ScalaPbAny.fromJavaProto(JsonSupport.encodeJson(other, lookupTypeHint(other)))
    }
  }

  override def encodeJava(value: Any): JavaPbAny = {
    value match {
      case javaPbAny: JavaPbAny   => javaPbAny
      case scalaPbAny: ScalaPbAny => ScalaPbAny.toJavaProto(scalaPbAny)
      case other                  => JsonSupport.encodeJson(other, lookupTypeHint(other))
    }
  }

  private def lookupTypeHint(value: Any): String =
    lookupTypeHint(value.getClass)

  private def lookupTypeHint(clz: Class[_]): String = {
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

  def decodeToJson(value: ScalaPbAny): Any = {
    if (value.typeUrl.startsWith(JsonSupport.KALIX_JSON)) {
      val any = ScalaPbAny.toJavaProto(value)

      val typeName = value.typeUrl.replace(JsonSupport.KALIX_JSON, "")
      JsonSupport.decodeJson(reversedCache.get(typeName), any)
    } else {
      value
    }
  }

}
