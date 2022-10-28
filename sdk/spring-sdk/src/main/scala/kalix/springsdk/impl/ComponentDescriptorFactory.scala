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
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

import kalix.EventDestination
import kalix.EventSource
import kalix.Eventing
import kalix.JwtMethodOptions
import kalix.MethodOptions
import kalix.javasdk.action.Action
import kalix.javasdk.view.View
import kalix.springsdk.annotations.EntityType
import kalix.springsdk.annotations.JWT
import kalix.springsdk.annotations.Publish
import kalix.springsdk.annotations.Subscribe
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.reflection._

private[impl] object ComponentDescriptorFactory {

  def hasValueEntitySubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.ValueEntity]) != null

  def hasEventSourcedEntitySubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.EventSourcedEntity]) != null

  def hasEventSourcedEntitySubscription(clazz: Class[_]): Boolean =
    Modifier.isPublic(clazz.getModifiers) &&
    clazz.getAnnotation(classOf[Subscribe.EventSourcedEntity]) != null

  def hasActionOutput(javaMethod: Method): Boolean = {
    javaMethod.getGenericReturnType match {
      case p: ParameterizedType =>
        p.getRawType.equals(classOf[Action.Effect[_]]) &&
          Modifier.isPublic(javaMethod.getModifiers)
      case _ => false
    }
  }

  def hasUpdateEffectOutput(javaMethod: Method): Boolean = {
    javaMethod.getGenericReturnType match {
      case p: ParameterizedType =>
        p.getRawType.equals(classOf[View.UpdateEffect[_]]) &&
          Modifier.isPublic(javaMethod.getModifiers)
      case _ => false
    }
  }

  def hasTopicSubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.Topic]) != null

  def hasTopicSubscription(clazz: Class[_]): Boolean =
    Modifier.isPublic(clazz.getModifiers) &&
    clazz.getAnnotation(classOf[Subscribe.Topic]) != null

  def hasTopicPublication(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Publish.Topic]) != null

  def hasJwtMethodOptions(javaMehod: Method): Boolean =
    Modifier.isPublic(javaMehod.getModifiers) &&
    javaMehod.getAnnotation(classOf[JWT]) != null

  private def findEventSourcedEntityType(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[EntityType]).value()
  }

  def findEventSourcedEntityType(clazz: Class[_]): String = {
    val ann = clazz.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[EntityType]).value()
  }

  def findValueEntityType(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[EntityType]).value()
  }

  def findValueEntityType(component: Class[_]): String = {
    val ann = component.getAnnotation(classOf[Subscribe.ValueEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[EntityType]).value()
  }

  private def findSubscriptionTopicName(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.Topic])
    ann.value()
  }

  private def findSubscriptionTopicName(clazz: Class[_]): String = {
    val ann = clazz.getAnnotation(classOf[Subscribe.Topic])
    ann.value()
  }

  def findSubscriptionConsumerGroup(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.Topic])
    ann.consumerGroup()
  }

  private def findSubscriptionConsumerGroup(clazz: Class[_]): String = {
    val ann = clazz.getAnnotation(classOf[Subscribe.Topic])
    ann.consumerGroup()
  }

  def findPublicationTopicName(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Publish.Topic])
    ann.value()
  }

  def hasIgnoreForTopic(clazz: Class[_]): Boolean = {
    val ann = clazz.getAnnotation(classOf[Subscribe.Topic])
    ann.ignoreUnknown()
  }

  def hasIgnoreForEventSourcedEntity(clazz: Class[_]): Boolean = {
    val ann = clazz.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    ann.ignoreUnkown()
  }

  def findIgnore(clazz: Class[_]): Boolean = {
    if (hasTopicSubscription(clazz)) hasIgnoreForTopic(clazz)
    else if (hasEventSourcedEntitySubscription(clazz)) hasIgnoreForEventSourcedEntity(clazz)
    else false
  }

  def jwtMethodOptions(javaMethod: Method): JwtMethodOptions = {
    val ann = javaMethod.getAnnotation(classOf[JWT])
    val jwt = JwtMethodOptions.newBuilder()
    ann
      .validate()
      .map(springValidate => jwt.addValidate(JwtMethodOptions.JwtMethodMode.forNumber(springValidate.ordinal())))
    ann
      .sign()
      .map(springSign => jwt.addSign(JwtMethodOptions.JwtMethodMode.forNumber(springSign.ordinal())))
    ann.bearerTokenIssuer().map(jwt.addBearerTokenIssuer)
    jwt.build()
  }

  def eventingInForValueEntity(javaMethod: Method): Eventing = {
    val entityType = findValueEntityType(javaMethod)
    val eventSource = EventSource.newBuilder().setValueEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForEventSourcedEntity(javaMethod: Method): Eventing = {
    val entityType = findEventSourcedEntityType(javaMethod)
    val eventSource = EventSource.newBuilder().setEventSourcedEntity(entityType).build()
    // ignore in method must be always false
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForEventSourcedEntity(clazz: Class[_]): Eventing = {
    val entityType = findEventSourcedEntityType(clazz)
    val eventSource = EventSource.newBuilder().setEventSourcedEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForTopic(javaMethod: Method): Eventing = {
    val topicName = findSubscriptionTopicName(javaMethod)
    val consumerGroup = findSubscriptionConsumerGroup(javaMethod)
    val eventSource = EventSource.newBuilder().setTopic(topicName).setConsumerGroup(consumerGroup).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForTopic(clazz: Class[_]): Eventing = {
    val topicName = findSubscriptionTopicName(clazz)
    val consumerGroup = findSubscriptionConsumerGroup(clazz)
    val eventSource =
      EventSource.newBuilder().setTopic(topicName).setConsumerGroup(consumerGroup).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingOutForTopic(javaMethod: Method): Eventing = {
    val topicName = findPublicationTopicName(javaMethod)
    val eventSource = EventDestination.newBuilder().setTopic(topicName).build()
    Eventing.newBuilder().setOut(eventSource).build()
  }

  def eventingInForValueEntity(entityType: String): Eventing = {
    val eventSource = EventSource.newBuilder().setValueEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def validateRestMethod(javaMethod: Method): Boolean =
    if (hasValueEntitySubscription(javaMethod) || hasEventSourcedEntitySubscription(javaMethod))
      throw new IllegalArgumentException(
        "Methods annotated with Kalix @Subscription annotations" +
        " can not be annotated with REST annotations ")
    else true

  // TODO: add more validations here
  // we should let users know if components are missing required annotations,
  // eg: entities require @EntityType, view require @Table and @Subscription
  def getFactoryFor(component: Class[_]): ComponentDescriptorFactory = {
    if (component.getAnnotation(classOf[EntityType]) != null)
      EntityDescriptorFactory
    else if (component.getAnnotation(classOf[Table]) != null)
      ViewDescriptorFactory
    else
      ActionDescriptorFactory
  }

  def combineByES(
      subscriptions: Seq[KalixMethod],
      messageCodec: SpringSdkMessageCodec,
      component: Class[_]): Seq[KalixMethod] = {
    def groupByES(methods: Seq[KalixMethod]): Map[String, Seq[KalixMethod]] = {
      val withEventSourcedIn = methods.filter(kalixMethod =>
        kalixMethod.methodOptions.exists(option =>
          option.hasEventing && option.getEventing.hasIn && option.getEventing.getIn.hasEventSourcedEntity))
      //Assuming there is only one eventing.in annotation per method, therefore head is as good as any other
      withEventSourcedIn.groupBy(m => m.methodOptions.head.getEventing.getIn.getEventSourcedEntity)
    }
    groupByES(subscriptions).collect {
      case (eventSourcedEntity, kMethods) if kMethods.size > 1 =>
        val methodsMap =
          kMethods.map { k =>
            val methodParameterTypes = k.serviceMethod.javaMethodOpt.get.getParameterTypes
            // it is safe to pick the last parameter. An action has one and View has two. In the View always the last is the event
            val eventParameter = methodParameterTypes.last

            val typeUrl = messageCodec.typeUrlFor(eventParameter)
            (typeUrl, k.serviceMethod.javaMethodOpt.get)
          }.toMap

        val subscriptions = kMethods.map(_.serviceMethod.asInstanceOf[SubscriptionServiceMethod])
        KalixMethod(
          CombinedSubscriptionServiceMethod(
            component.getName,
            "KalixSyntheticMethodOnES" + eventSourcedEntity.capitalize,
            methodsMap))
          .withKalixOptions(kMethods.head.methodOptions)
      case (eventSourcedEntity, kMethod +: Nil) =>
        kMethod
    }.toSeq
  }

  private[impl] def buildJWTOptions(method: Method): Option[MethodOptions] = {
    Option.when(hasJwtMethodOptions(method)) {
      kalix.MethodOptions.newBuilder().setJwt(jwtMethodOptions(method)).build()
    }
  }
}

private[impl] trait ComponentDescriptorFactory {

  /**
   * Inspect the component class (type), validate the annotations/methods and build a component descriptor for it.
   */
  def buildDescriptorFor(
      componentClass: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor

}

/**
 * Thrown when the component has incorrect annotations
 */
final case class InvalidComponentException(message: String) extends RuntimeException(message)
