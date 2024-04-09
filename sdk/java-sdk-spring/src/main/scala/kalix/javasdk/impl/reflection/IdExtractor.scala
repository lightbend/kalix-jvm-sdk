/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.reflection

import java.lang.reflect.AnnotatedElement

import java.lang.reflect.Method

import kalix.javasdk.annotations.GenerateId
import kalix.javasdk.annotations.Id

object IdExtractor {

  private[kalix] def shouldGenerateId(annotatedElement: AnnotatedElement) =
    annotatedElement.getAnnotation(classOf[GenerateId]) != null

  def extractIds(component: Class[_], method: Method): Seq[String] = {

    def idValue(annotatedElement: AnnotatedElement) =
      if (annotatedElement.getAnnotation(classOf[Id]) != null)
        annotatedElement.getAnnotation(classOf[Id]).value()
      else
        Array.empty[String]

    val idsOnType = idValue(component)
    val idsOnMethod = idValue(method)

    if (shouldGenerateId(method)) {
      if (idsOnMethod.nonEmpty)
        throw ServiceIntrospectionException(
          method,
          "Invalid annotation usage. Found both @Id and @GenerateId annotations. " +
          "A method can only be annotated with one of them, but not both.")
      else {
        Seq.empty
      }
    } else {
      // ids defined on Method level get precedence
      val idsToUse =
        if (idsOnMethod.nonEmpty) idsOnMethod
        else idsOnType

      if (idsToUse.isEmpty)
        throw ServiceIntrospectionException(
          method,
          "Invalid command method. No @Id nor @GenerateId annotations found. " +
          "A command method should be annotated with either @Id or @GenerateId, or " +
          "an @Id annotation should be present at class level.")

      idsToUse.toIndexedSeq
    }
  }
}
