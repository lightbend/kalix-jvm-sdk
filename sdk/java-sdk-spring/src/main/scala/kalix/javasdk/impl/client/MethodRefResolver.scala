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

package kalix.javasdk.impl.client

import java.lang.invoke.SerializedLambda
import java.lang.reflect.Method

private[kalix] object MethodRefResolver {

  /**
   * Resolve the method ref for a lambda.
   */
  def resolveMethodRef(lambda: Any): Method = {
    val lambdaType = lambda.getClass

    if (!classOf[java.io.Serializable].isInstance(lambda)) {
      throw new IllegalArgumentException(
        "Can only resolve method references from serializable SAMs, class was: " + lambdaType)
    }

    val writeReplace =
      try {
        lambda.getClass.getDeclaredMethod("writeReplace")
      } catch {
        case e: NoSuchMethodError =>
          throw new IllegalArgumentException(
            "Passed in object does not provide a writeReplace method, hence it can't be a Java 8 method reference.",
            e)
      }

    writeReplace.setAccessible(true)

    val serializedLambda = writeReplace.invoke(lambda) match {
      case s: SerializedLambda => s
      case other =>
        throw new IllegalArgumentException(
          "Passed in object does not writeReplace itself with SerializedLambda, hence it can't be a Java 8 method reference.")
    }

    // Try to load the class that the method ref is defined on
    val ownerClass = loadClass(lambdaType.getClassLoader, serializedLambda.getImplClass)

    val argumentClasses = getArgumentClasses(lambdaType.getClassLoader, serializedLambda.getImplMethodSignature)
    if (serializedLambda.getImplClass.equals("<init>")) {
      throw new IllegalArgumentException("Passed in method ref is a constructor.")
    } else {
      ownerClass.getDeclaredMethod(serializedLambda.getImplMethodName, argumentClasses: _*)
    }
  }

  private def loadClass(classLoader: ClassLoader, internalName: String) = {
    Class.forName(internalName.replace('/', '.'), false, classLoader)
  }

  private def getArgumentClasses(classLoader: ClassLoader, methodDescriptor: String): List[Class[_]] = {
    def parseArgumentClasses(offset: Int, arrayDepth: Int): List[Class[_]] = {
      methodDescriptor.charAt(offset) match {
        case ')' => Nil
        case 'L' =>
          val end = methodDescriptor.indexOf(';', offset)
          val className = if (arrayDepth > 0) {
            methodDescriptor.substring(offset - arrayDepth, end)
          } else {
            methodDescriptor.substring(offset + 1, end)
          }
          loadClass(classLoader, className) :: parseArgumentClasses(end + 1, 0)
        case '[' =>
          parseArgumentClasses(offset + 1, arrayDepth + 1)
        case _ if arrayDepth > 0 =>
          val className = methodDescriptor.substring(offset - arrayDepth, offset + 1)
          loadClass(classLoader, className) :: parseArgumentClasses(offset + 1, 0)
        case other =>
          val clazz = other match {
            case 'Z'     => classOf[Boolean]
            case 'C'     => classOf[Char]
            case 'B'     => classOf[Byte]
            case 'S'     => classOf[Short]
            case 'I'     => classOf[Int]
            case 'F'     => classOf[Float]
            case 'J'     => classOf[Long]
            case 'D'     => classOf[Double]
            case unknown => throw sys.error("Unknown primitive type: " + unknown)
          }
          clazz :: parseArgumentClasses(offset + 1, 0)
      }
    }

    parseArgumentClasses(1, 0)
  }
}
