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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.MessageCodec

private[springsdk] class SpringSdkMessageCodec extends MessageCodec {

  private val cache: ConcurrentMap[Class[_], String] = new ConcurrentHashMap()

  /**
   * In the Spring SDK, output data are encoded to Json.
   */
  override def encodeScala(value: Any): ScalaPbAny =
    ScalaPbAny.fromJavaProto(JsonSupport.encodeJson(value, lookupTypeHint(value)))

  override def encodeJava(value: Any): JavaPbAny =
    JsonSupport.encodeJson(value, lookupTypeHint(value))

  private def lookupTypeHint(value: Any): String =
    lookupTypeHint(value.getClass)

  private def lookupTypeHint(clz: Class[_]): String =
    cache.computeIfAbsent(clz, clz => SpringSdkMessageCodec.findTypeHint(clz))

  def typeUrlFor(clz: Class[_]) =
    JsonSupport.KALIX_JSON + lookupTypeHint(clz)

  /**
   * In the Spring SDK, input data are kept as proto Any and delivered as such to the router
   */
  override def decodeMessage(value: ScalaPbAny): Any = value

}

private[springsdk] object SpringSdkMessageCodec {

  /**
   * Used in to compute cache value if absent. This method will try to scan the type hierarchy from the passed
   * `messageClass` to for either an explicit JsonTypeName annotation or a JsonSubTypes.
   *
   * In the absence of any annotation from the JsonTypeInfo family, it will fallback to use the FQCN as a type hint.
   */
  private def findTypeHint(messageClass: Class[_]): String = {

    def annotatedParents(clz: Class[_], listOfParents: Seq[Class[_]]): Seq[Class[_]] = {

      def hasJsonSubTypes(clz: Class[_]) =
        clz != null && clz.getAnnotation(classOf[JsonSubTypes]) != null

      val acc =
        if (hasJsonSubTypes(clz)) listOfParents :+ clz
        else listOfParents

      val directParents = clz.getSuperclass +: clz.getInterfaces

      directParents.foldLeft(acc) { case (acc, clz) =>
        if (clz == null) acc // happens when we reach the bottom, ie: Object.getSuperclass == null
        else annotatedParents(clz, acc)
      }
    }

    // straightforward case: class is annotated with JsonTypeName
    if (messageClass.getAnnotation(classOf[JsonTypeName]) != null) {
      messageClass.getAnnotation(classOf[JsonTypeName]).value()
    } else {
      // otherwise needs to scan hierarchy until we find JsonSubTypes annotations
      // in a parent class or trait
      val parents = annotatedParents(messageClass, Seq.empty)
      if (parents.isEmpty)
        messageClass.getName
      else {
        val ann = {
          parents.flatMap { parent =>
            val subTypeAnn = parent.getAnnotation(classOf[JsonSubTypes])
            subTypeAnn.value().find(_.value() == messageClass)
          }
        }.headOption

        ann
          .map { a =>
            // if more than one name, we pick the first one for the typeUrl
            // otherwise, default to `name`
            a.names().headOption.getOrElse(a.name())
          }
          .getOrElse(messageClass.getName)
      }
    }
  }
}
