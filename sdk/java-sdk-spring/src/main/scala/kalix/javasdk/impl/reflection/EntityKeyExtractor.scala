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

package kalix.javasdk.impl.reflection

import kalix.javasdk.annotations.EntityKey
import kalix.javasdk.annotations.GenerateEntityKey

import java.lang.reflect.Method

object EntityKeyExtractor {

  def extractEntityKeys(component: Class[_], method: Method): Seq[String] = {
    val entityKeysOnType = {
      val anno = component.getAnnotation(classOf[EntityKey])
      if (anno != null) anno.value()
      else Array.empty[String]
    }

    val entityKeyOnMethod = method.getAnnotation(classOf[EntityKey])
    val generateEntityKey = method.getAnnotation(classOf[GenerateEntityKey])

    if (entityKeyOnMethod != null && generateEntityKey != null)
      throw ServiceIntrospectionException(
        method,
        "Invalid annotation usage. Found both @EntityKey and @GenerateEntityKey annotations. " +
        "A method can only be annotated with one of them, but not both.")

    // keys defined on Method level get precedence
    val entityKeysToUse =
      if (entityKeyOnMethod != null) entityKeyOnMethod.value()
      else entityKeysOnType

    if (entityKeysToUse.isEmpty && generateEntityKey == null)
      throw ServiceIntrospectionException(
        method,
        "Invalid command method. No @EntityKey nor @GenerateEntityKey annotations found. " +
        "A command method should be annotated with either @EntityKey or @GenerateEntityKey, or " +
        "an @EntityKey annotation should be present at class level.")

    entityKeysToUse.toIndexedSeq
  }
}
