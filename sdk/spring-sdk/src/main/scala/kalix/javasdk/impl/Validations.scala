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

package kalix.javasdk.impl

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import scala.reflect.ClassTag

import kalix.javasdk.action.Action
import kalix.javasdk.annotations.Publish
import kalix.javasdk.annotations.Query
import kalix.javasdk.annotations.Subscribe
import kalix.javasdk.annotations.Table
import kalix.javasdk.impl.ComponentDescriptorFactory.MethodOps
import kalix.javasdk.impl.ComponentDescriptorFactory.hasAcl
import kalix.javasdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.javasdk.impl.ComponentDescriptorFactory.hasRestAnnotation
import kalix.javasdk.impl.ComponentDescriptorFactory.hasSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.javasdk.impl.reflection.ServiceMethod
import kalix.javasdk.view.View
import kalix.spring.impl.KalixSpringApplication
// TODO: abstract away spring and reactor dependencies
import org.springframework.web.bind.annotation.RequestBody
import reactor.core.publisher.Flux

object Validations {

  object Validation {

    def apply(messages: Array[String]): Validation = Validation(messages.toIndexedSeq)

    def apply(messages: Seq[String]): Validation =
      if (messages.isEmpty) Valid
      else Invalid(messages)
  }

  sealed trait Validation {
    def isValid: Boolean
    final def isInvalid: Boolean = !isInvalid
    def ++(validation: Validation): Validation

    def failIfInvalid: Validation
  }

  case object Valid extends Validation {
    override def isValid: Boolean = true
    override def ++(validation: Validation): Validation = validation

    override def failIfInvalid: Validation = this
  }

  object Invalid {
    def apply(message: String): Invalid =
      Invalid(Seq(message))
  }

  case class Invalid(messages: Seq[String]) extends Validation {
    override def isValid: Boolean = false

    override def ++(validation: Validation): Validation =
      validation match {
        case Valid      => this
        case i: Invalid => Invalid(this.messages ++ i.messages)
      }

    override def failIfInvalid: Validation =
      throw InvalidComponentException(messages.mkString(", "))

  }

  private def when(cond: Boolean)(block: => Validation): Validation =
    if (cond) block else Valid

  private def when[T: ClassTag](component: Class[_])(block: => Validation): Validation =
    if (assignable[T](component)) block else Valid

  private def assignable[T: ClassTag](component: Class[_]): Boolean =
    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]].isAssignableFrom(component)

  private def commonValidation(component: Class[_]): Validation = {
    noRestStreamIn(component)
  }

  private def commonSubscriptionValidation(component: Class[_]): Validation = {
    eventSourcedEntitySubscriptionValidations(component) ++
    valueEntitySubscriptionValidations(component) ++
    topicSubscriptionValidations(component) ++
    publishStreamIdMustBeFilled(component) ++
    noSubscriptionMethodWithAcl(component) ++
    noSubscriptionWithRestAnnotations(component) ++
    subscriptionMethodMustHaveOneParameter(component)
  }

  def validate(component: Class[_]): Validation =
    validateAction(component) ++
    validateView(component)

  private def validateAction(component: Class[_]): Validation = {
    when[Action](component) {
      commonValidation(component) ++ commonSubscriptionValidation(component)
    }
  }

  private def validateView(component: Class[_]): Validation = {
    when[View[_]](component) {
      when(!KalixSpringApplication.isNestedViewTable(component)) {
        viewMustHaveOneQueryMethod(component)
      } ++
      commonValidation(component) ++
      commonSubscriptionValidation(component) ++
      viewMustHaveTableName(component) ++
      viewMustHaveMethodLevelSubscriptionWhenTransformingUpdates(component) ++
      streamUpdatesQueryMustReturnFlux(component)
    } ++
    when(KalixSpringApplication.isMultiTableView(component)) {
      viewMustHaveOneQueryMethod(component)
    }
  }

  private def errorMessage(element: AnnotatedElement, message: String): String = {
    val elementStr =
      element match {
        case clz: Class[_] => clz.getName
        case meth: Method  => s"${meth.getDeclaringClass.getName}#${meth.getName}"
        case any           => any.toString
      }
    s"On '$elementStr': $message"
  }

  private def eventSourcedEntitySubscriptionValidations(component: Class[_]): Validation = {
    val methods = component.getMethods.toIndexedSeq
    when(
      hasEventSourcedEntitySubscription(component) &&
      methods.exists(hasEventSourcedEntitySubscription)) {
      // collect offending methods
      val messages = methods.filter(hasEventSourcedEntitySubscription).map { method =>
        errorMessage(
          method,
          "You cannot use @Subscribe.EventSourcedEntity annotation in both methods and class. You can do either one or the other.")
      }
      Validation(messages)
    }
  }

  private def topicSubscriptionValidations(component: Class[_]): Validation = {
    val methods = component.getMethods.toIndexedSeq
    when(hasTopicSubscription(component) && methods.exists(hasTopicSubscription)) {
      // collect offending methods
      val messages = methods.filter(hasTopicSubscription).map { method =>
        errorMessage(
          method,
          "You cannot use @Subscribe.Topic annotation in both methods and class. You can do either one or the other.")
      }
      Validation(messages)
    }
  }

  private def publishStreamIdMustBeFilled(component: Class[_]): Validation = {
    Option(component.getAnnotation(classOf[Publish.Stream]))
      .map { ann =>
        when(ann.id().trim.isEmpty) {
          Validation(Seq("@Publish.Stream id can not be an empty string"))
        }
      }
      .getOrElse(Valid)
  }

  private def noSubscriptionMethodWithAcl(component: Class[_]): Validation = {

    val hasSubscriptionAndAcl = (method: Method) => hasAcl(method) && hasSubscription(method)

    val messages =
      component.getMethods.toIndexedSeq.filter(hasSubscriptionAndAcl).map { method =>
        errorMessage(
          method,
          "Methods annotated with Kalix @Subscription annotations are for internal use only and cannot be annotated with ACL annotations.")
      }

    Validation(messages)
  }

  private def noSubscriptionWithRestAnnotations(component: Class[_]): Validation = {

    val hasSubscriptionAndRest = (method: Method) => hasRestAnnotation(method) && hasSubscription(method)

    val messages =
      component.getMethods.toIndexedSeq.filter(hasSubscriptionAndRest).map { method =>
        errorMessage(
          method,
          "Methods annotated with Kalix @Subscription annotations are for internal use only and cannot be annotated with REST annotations.")
      }

    Validation(messages)
  }

  private def noRestStreamIn(component: Class[_]): Validation = {

    // this is more for early validation. We don't support stream-in over http,
    // we block it before deploying anything
    def isStreamIn(method: Method): Boolean = {
      val paramWithRequestBody =
        method.getParameters.collect {
          case param if param.getAnnotation(classOf[RequestBody]) != null => param
        }
      paramWithRequestBody.exists(_.getType == classOf[Flux[_]])
    }

    val hasRestWithStreamIn = (method: Method) => hasRestAnnotation(method) && isStreamIn(method)

    val messages =
      component.getMethods.filter(hasRestWithStreamIn).map { method =>
        errorMessage(method, "Stream in calls are not supported.")
      }

    Validation(messages)
  }

  private def viewMustHaveTableName(component: Class[_]): Validation = {
    val ann = component.getAnnotation(classOf[Table])
    if (ann == null) {
      Invalid(errorMessage(component, "A View should be annotated with @Table."))
    } else {
      val tableName: String = ann.value()
      if (tableName == null || tableName.trim.isEmpty) {
        Invalid(errorMessage(component, "@Table name is empty, must be a non-empty string."))
      } else Valid
    }
  }

  private def viewMustHaveMethodLevelSubscriptionWhenTransformingUpdates(component: Class[_]): Validation = {
    if (hasValueEntitySubscription(component)) {
      val tableType: Class[_] = tableTypeOf(component)
      val valueEntityClass: Class[_] =
        component.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]
      val entityStateClass = valueEntityStateClassOf(valueEntityClass)

      when(entityStateClass != tableType) {
        val message =
          s"You are using a type level annotation in this View and that requires the View type [${tableType.getName}] " +
          s"to match the ValueEntity type [${entityStateClass.getName}]. " +
          s"If your intention is to transform the type, you should instead add a method like " +
          s"`UpdateEffect<${tableType.getName}> onChange(${entityStateClass.getName} state)`" +
          " and move the @Subscribe.ValueEntity to it."

        Validation(Seq(errorMessage(component, message)))
      }
    } else {
      Valid
    }
  }

  private def viewMustHaveOneQueryMethod(component: Class[_]): Validation = {

    val annotatedQueryMethods =
      component.getMethods
        .filter(_.hasAnnotation[Query])
        .filter(hasRestAnnotation)
        .toList

    annotatedQueryMethods match {
      case Nil =>
        Invalid(
          errorMessage(
            component,
            "No valid query method found. Views should have a method annotated with @Query and exposed by a REST annotation."))
      case head :: Nil => Valid
      case _ =>
        val messages =
          annotatedQueryMethods.map { method =>
            errorMessage(method, "Views can have only one method annotated with @Query.")
          }
        Invalid(messages)
    }
  }

  private def streamUpdatesQueryMustReturnFlux(component: Class[_]): Validation = {

    val offendingMethods =
      component.getMethods
        .filter(hasRestAnnotation)
        .filter(_.hasAnnotation[Query])
        .filter { method =>
          method.getAnnotation(classOf[Query]).streamUpdates() && !ServiceMethod.isStreamOut(method)
        }

    val messages = offendingMethods.map { method =>
      errorMessage(method, "@Query.streamUpdates can only be enabled in stream methods returning Flux")
    }

    Validation(messages)
  }

  private def subscriptionMethodMustHaveOneParameter(component: Class[_]): Validation = {
    val offendingMethods = component.getMethods
      .filter(hasValueEntitySubscription)
      .filterNot(hasHandleDeletes)
      .filter(_.getParameterTypes.isEmpty) //maybe a delete handler

    val messages =
      offendingMethods.map { method =>
        errorMessage(method, "Subscription method must have one parameter, unless it's marked as handleDeletes.")
      }

    Validation(messages)
  }

  private def valueEntitySubscriptionValidations(component: Class[_]): Validation = {

    val subscriptionMethods = component.getMethods.toIndexedSeq.filter(hasValueEntitySubscription)
    val updatedMethods = subscriptionMethods.filterNot(hasHandleDeletes).filter(_.getParameterTypes.nonEmpty)

    val (handleDeleteMethods, handleDeleteMethodsWithParam) =
      subscriptionMethods.filter(hasHandleDeletes).partition(_.getParameterTypes.isEmpty)

    val noMixedLevelValueEntitySubscription =
      when(hasValueEntitySubscription(component) && subscriptionMethods.nonEmpty) {
        // collect offending methods
        val messages = subscriptionMethods.map { method =>
          errorMessage(
            method,
            "You cannot use @Subscribe.ValueEntity annotation in both methods and class. You can do either one or the other.")
        }
        Validation(messages)
      }

    val handleDeletesMustHaveZeroArity = {
      val messages =
        handleDeleteMethodsWithParam.map { method =>
          val numParams = method.getParameters.length
          errorMessage(
            method,
            s"Method annotated with '@Subscribe.ValueEntity' and handleDeletes=true must not have parameters. Found ${numParams} method parameters.")
        }

      Validation(messages)
    }

    val onlyOneValueEntityUpdateIsAllowed = {
      if (updatedMethods.size >= 2) {
        val messages =
          updatedMethods.map { method =>
            errorMessage(method, "Duplicated update methods for ValueEntity subscription.")
          }
        Validation(messages)
      } else Valid
    }

    val onlyOneHandlesDeleteIsAllowed = {
      val offendingMethods = handleDeleteMethods.filter(_.getParameterTypes.isEmpty)

      if (offendingMethods.size >= 2) {
        val messages =
          offendingMethods.map { method =>
            errorMessage(
              method,
              "Multiple methods annotated with @Subscription.ValueEntity(handleDeletes=true) is not allowed.")
          }
        Validation(messages)
      } else Valid
    }

    val standaloneMethodLevelHandleDeletesIsNotAllowed = {
      if (handleDeleteMethods.nonEmpty && updatedMethods.isEmpty) {
        val messages =
          handleDeleteMethods.map { method =>
            errorMessage(method, "Method annotated with handleDeletes=true has no matching update method.")
          }
        Validation(messages)
      } else Valid
    }

    noMixedLevelValueEntitySubscription ++
    handleDeletesMustHaveZeroArity ++
    onlyOneValueEntityUpdateIsAllowed ++
    onlyOneHandlesDeleteIsAllowed ++
    standaloneMethodLevelHandleDeletesIsNotAllowed
  }

  private def tableTypeOf(component: Class[_]) = {
    component.getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments
      .head
      .asInstanceOf[Class[_]]
  }

  private def valueEntityStateClassOf(valueEntityClass: Class[_]): Class[_] = {
    valueEntityClass.getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments
      .head
      .asInstanceOf[Class[_]]
  }

}
