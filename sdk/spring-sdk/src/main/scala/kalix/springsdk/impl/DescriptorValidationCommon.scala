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

import java.lang.reflect.Method

import kalix.javasdk.valueentity
import kalix.springsdk.annotations.Subscribe.ValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription

object DescriptorValidationCommon {

  def validateHandleDeletesMethodArity(method: Method): Unit = {
    method.getParameterTypes.toList match {
      case params if params.nonEmpty =>
        throw InvalidComponentException(
          s"Method [${method.getName}] annotated with '@Subscribe.ValueEntity' and handleDeletes=true must not have parameters. Found ${params.size} method parameters.")
      case _ => // happy days, dev did good with the signature
    }
  }

  def validateHandleDeletesTrueOnMethodLevel(component: Class[_]) = {
    val incorrectDeleteHandlers = component.getMethods
      .filter(hasValueEntitySubscription)
      .filter(_.getParameterTypes.isEmpty) //maybe a delete handler
      .filterNot(hasHandleDeletes)
      .map(_.getName)

    if (incorrectDeleteHandlers.nonEmpty) {
      throw InvalidComponentException(
        s"Methods: '${incorrectDeleteHandlers.mkString(", ")}' look like delete handlers but with handleDeletes flag eq false. " +
        s"Change flag to true, or fix method signature to accept one parameter.")
    }
  }

  def validateIfHandleDeletesMethodsMatchesSubscriptions(component: Class[_]) = {
    val handleDeletesValueEntityClasses = component.getMethods
      .filter(hasHandleDeletes)
      .map(method => (method.getName, method.getAnnotation(classOf[ValueEntity]).value()))
      .toMap

    val valueEntitySubscriptionMethods = component.getMethods
      .filterNot(hasHandleDeletes)
      .filter(hasValueEntitySubscription)
      .map(method => (method.getName, method.getAnnotation(classOf[ValueEntity]).value()))
      .toMap

    validateDuplicatedClasses(handleDeletesValueEntityClasses)
    validateDuplicatedClasses(valueEntitySubscriptionMethods)

    val deletesVEClasses = handleDeletesValueEntityClasses.values.map(_.getName).toSet
    val subscriptionVEClasses = valueEntitySubscriptionMethods.values.map(_.getName).toSet

    val diff = deletesVEClasses.diff(subscriptionVEClasses)

    if (diff.nonEmpty) {
      throw InvalidComponentException(
        s"Some methods annotated with handleDeletes=true don't have matching subscription methods. " +
        s"Add subscription annotated with '@Subscribe.ValueEntity' for types: ${diff.mkString(", ")}")
    }
  }

  def validateDuplicatedClasses(methodWithValueEntityClass: Map[String, Class[_ <: valueentity.ValueEntity[_]]]) = {
    methodWithValueEntityClass.groupBy(_._2.getName).map {
      case (className, groupedMethods) if groupedMethods.size > 1 =>
        throw new InvalidComponentException(
          s"Duplicated subscription to the same ValueEntity: $className from methods: ${groupedMethods.keys.mkString(", ")}")
      case _ => //ok
    }
  }

}
