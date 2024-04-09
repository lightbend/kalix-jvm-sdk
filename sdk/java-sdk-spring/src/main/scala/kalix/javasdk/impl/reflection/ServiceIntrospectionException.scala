/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
