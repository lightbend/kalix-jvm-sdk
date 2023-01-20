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

  /**
   * In the Spring SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): ScalaPbAny = {
    if (value == null) throw NullSerializationException
    ScalaPbAny.fromJavaProto(JsonSupport.encodeJson(value, lookupTypeHint(value)))
  }

  override def encodeJava(value: Any): JavaPbAny = {
    if (value == null) throw NullSerializationException
    JsonSupport.encodeJson(value, lookupTypeHint(value))
  }

  private def lookupTypeHint(value: Any): String =
    lookupTypeHint(value.getClass)

  private def lookupTypeHint(clz: Class[_]): String =
    cache.computeIfAbsent(
      clz,
      clz => {
        Option(clz.getAnnotation(classOf[TypeName]))
          .collect { case ann if ann.value().trim.nonEmpty => ann.value() }
          .getOrElse(clz.getSimpleName)
      })

  def typeUrlFor(clz: Class[_]) =
    JsonSupport.KALIX_JSON + lookupTypeHint(clz)

  /**
   * In the Spring SDK, input data are kept as proto Any and delivered as such to the router
   */
  override def decodeMessage(value: ScalaPbAny): Any = value

}
