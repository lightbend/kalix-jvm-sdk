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

package kalix.javasdk.impl.reflection

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

object ServiceIntrospectionException {
  def apply(element: AnnotatedElement, msg: String): ServiceIntrospectionException = {
    val elementStr =
      element match {
        case clz: Class[_] => clz.getName
        case meth: Method  => s"${meth.getDeclaringClass.getName}#${meth.getName}"
        case any           => any.toString
      }

    new ServiceIntrospectionException(s"On $elementStr: $msg")
  }

}

class ServiceIntrospectionException private (msg: String) extends RuntimeException(msg)
