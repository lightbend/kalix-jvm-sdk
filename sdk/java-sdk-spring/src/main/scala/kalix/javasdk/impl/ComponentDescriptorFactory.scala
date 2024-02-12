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

package kalix.javasdk.impl

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kalix.DirectDestination
import kalix.DirectSource
import kalix.EventDestination
import kalix.EventSource
import kalix.Eventing
import kalix.MethodOptions
import kalix.ServiceEventing
import kalix.ServiceEventingOut
import kalix.ServiceOptions
import kalix.javasdk.action.Action
import kalix.javasdk.annotations.Acl
import kalix.javasdk.annotations.EntityType
import kalix.javasdk.annotations.Publish
import kalix.javasdk.annotations.Subscribe
import kalix.javasdk.annotations.Table
import kalix.javasdk.annotations.TypeId
import kalix.javasdk.annotations.ViewId
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.impl.reflection.CombinedSubscriptionServiceMethod
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.valueentity.ValueEntity
import kalix.javasdk.view.View
// TODO: abstract away spring dependency
import kalix.javasdk.impl.Reflect.Syntax._
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
private[impl] object ComponentDescriptorFactory {

  def hasRestAnnotation(javaMethod: Method): Boolean = {
    val restAnnotation = AnnotatedElementUtils.findMergedAnnotation(javaMethod, classOf[RequestMapping])
    javaMethod.isPublic && restAnnotation != null
  }

  def hasAcl(javaMethod: Method): Boolean =
    javaMethod.isPublic && javaMethod.hasAnnotation[Acl]

  def hasValueEntitySubscription(clazz: Class[_]): Boolean =
    clazz.isPublic && clazz.hasAnnotation[Subscribe.ValueEntity]

  def hasValueEntitySubscription(javaMethod: Method): Boolean =
    javaMethod.isPublic && javaMethod.hasAnnotation[Subscribe.ValueEntity]

  def hasEventSourcedEntitySubscription(javaMethod: Method): Boolean =
    javaMethod.isPublic && javaMethod.hasAnnotation[Subscribe.EventSourcedEntity]

  def hasEventSourcedEntitySubscription(clazz: Class[_]): Boolean =
    clazz.isPublic && clazz.hasAnnotation[Subscribe.EventSourcedEntity]

  def streamSubscription(clazz: Class[_]): Option[Subscribe.Stream] =
    clazz.getAnnotationOption[Subscribe.Stream]

  def hasSubscription(javaMethod: Method): Boolean = {
    hasValueEntitySubscription(javaMethod) ||
    hasEventSourcedEntitySubscription(javaMethod) ||
    hasTopicSubscription(javaMethod)
  }

  def hasSubscription(clazz: Class[_]): Boolean = {
    hasValueEntitySubscription(clazz) ||
    hasEventSourcedEntitySubscription(clazz) ||
    hasTopicSubscription(clazz) ||
    hasStreamSubscription(clazz)
  }

  private def valueEntitySubscription(clazz: Class[_]): Option[Subscribe.ValueEntity] =
    clazz.getAnnotationOption[Subscribe.ValueEntity]

  def eventSourcedEntitySubscription(clazz: Class[_]): Option[Subscribe.EventSourcedEntity] =
    clazz.getAnnotationOption[Subscribe.EventSourcedEntity]

  def topicSubscription(clazz: Class[_]): Option[Subscribe.Topic] =
    clazz.getAnnotationOption[Subscribe.Topic]

  def hasActionOutput(javaMethod: Method): Boolean = {
    if (javaMethod.isPublic) {
      javaMethod.getGenericReturnType match {
        case p: ParameterizedType => p.getRawType.equals(classOf[Action.Effect[_]])
        case _                    => false
      }
    } else {
      false
    }
  }

  def hasUpdateEffectOutput(javaMethod: Method): Boolean = {
    if (javaMethod.isPublic) {
      javaMethod.getGenericReturnType match {
        case p: ParameterizedType => p.getRawType.equals(classOf[View.UpdateEffect[_]])
        case _                    => false
      }
    } else {
      false
    }
  }

  def hasTopicSubscription(javaMethod: Method): Boolean =
    javaMethod.isPublic && javaMethod.hasAnnotation[Subscribe.Topic]

  def hasHandleDeletes(javaMethod: Method): Boolean = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    javaMethod.isPublic && ann != null && ann.handleDeletes()
  }

  def hasTopicSubscription(clazz: Class[_]): Boolean =
    clazz.isPublic && clazz.hasAnnotation[Subscribe.Topic]

  def hasStreamSubscription(clazz: Class[_]): Boolean =
    clazz.isPublic && clazz.hasAnnotation[Subscribe.Stream]

  def hasTopicPublication(javaMethod: Method): Boolean =
    javaMethod.isPublic && javaMethod.hasAnnotation[Publish.Topic]

  def readTypeIdValue(annotated: AnnotatedElement) =
    Option(annotated.getAnnotation(classOf[TypeId]))
      .map(_.value())
      .getOrElse {
        // assuming that if TypeId is not in use, EntityType will
        annotated.getAnnotation(classOf[EntityType]).value()
      }

  def findEventSourcedEntityType(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    readTypeIdValue(ann.value())
  }

  def findEventSourcedEntityClass(javaMethod: Method): Class[_ <: EventSourcedEntity[_, _]] = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    ann.value()
  }

  private def findValueEntityClass(javaMethod: Method): Class[_ <: ValueEntity[_]] = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    ann.value()
  }

  def findSubscriptionSourceName(javaMethod: Method): String = {
    if (hasValueEntitySubscription(javaMethod)) {
      findValueEntityClass(javaMethod).getName
    } else if (hasEventSourcedEntitySubscription(javaMethod)) {
      findEventSourcedEntityClass(javaMethod).getName
    } else if (hasTopicSubscription(javaMethod)) {
      "Topic-" + findSubscriptionTopicName(javaMethod)
    } else {
      throw new IllegalStateException("Unsupported source for " + javaMethod.getName)
    }
  }

  def findEventSourcedEntityType(clazz: Class[_]): String = {
    val ann = clazz.getAnnotation(classOf[Subscribe.EventSourcedEntity])
    readTypeIdValue(ann.value())
  }

  def findValueEntityType(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    readTypeIdValue(ann.value())
  }

  def findValueEntityType(component: Class[_]): String = {
    val ann = component.getAnnotation(classOf[Subscribe.ValueEntity])
    readTypeIdValue(ann.value())
  }

  def findHandleDeletes(javaMethod: Method): Boolean = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    ann.handleDeletes()
  }

  def findHandleDeletes(component: Class[_]): Boolean = {
    val ann = component.getAnnotation(classOf[Subscribe.ValueEntity])
    ann.handleDeletes()
  }

  def findSubscriptionTopicName(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.Topic])
    ann.value()
  }

  def findSubscriptionTopicName(clazz: Class[_]): String = {
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
    ann.ignoreUnknown()
  }

  def findIgnore(clazz: Class[_]): Boolean = {
    if (hasTopicSubscription(clazz)) hasIgnoreForTopic(clazz)
    else if (hasEventSourcedEntitySubscription(clazz)) hasIgnoreForEventSourcedEntity(clazz)
    else false
  }

  def eventingInForValueEntity(javaMethod: Method): Eventing = {
    val eventSource: EventSource = valueEntityEventSource(javaMethod)
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def valueEntityEventSource(javaMethod: Method) = {
    val entityType = findValueEntityType(javaMethod)
    EventSource
      .newBuilder()
      .setValueEntity(entityType)
      .setHandleDeletes(findHandleDeletes(javaMethod))
      .build()
  }

  def topicEventDestination(javaMethod: Method): Option[EventDestination] = {
    if (hasTopicPublication(javaMethod)) {
      val topicName = findPublicationTopicName(javaMethod)
      Some(EventDestination.newBuilder().setTopic(topicName).build())
    } else {
      None
    }
  }

  def eventingInForEventSourcedEntity(javaMethod: Method): Eventing = {
    val eventSource: EventSource = eventSourceEntityEventSource(javaMethod)
    // ignore in method must be always false
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventSourceEntityEventSource(javaMethod: Method) = {
    val entityType = findEventSourcedEntityType(javaMethod)
    EventSource.newBuilder().setEventSourcedEntity(entityType).build()
  }

  def eventingInForEventSourcedEntity(clazz: Class[_]): Eventing = {
    val entityType = findEventSourcedEntityType(clazz)
    val eventSource = EventSource.newBuilder().setEventSourcedEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForTopic(clazz: Class[_]): Eventing = {
    Eventing.newBuilder().setIn(topicEventSource(clazz)).build()
  }

  def eventingInForTopic(javaMethod: Method): Eventing = {
    Eventing.newBuilder().setIn(topicEventSource(javaMethod)).build()
  }

  def eventingInForValueEntityServiceLevel(clazz: Class[_]): Option[kalix.ServiceOptions] = {
    valueEntitySubscription(clazz).map { _ =>
      val entityType = findValueEntityType(clazz)
      val in = EventSource.newBuilder().setValueEntity(entityType)
      val eventing = ServiceEventing.newBuilder().setIn(in)
      kalix.ServiceOptions.newBuilder().setEventing(eventing).build()
    }
  }

  def eventingInForEventSourcedEntityServiceLevel(clazz: Class[_]): Option[kalix.ServiceOptions] = {
    eventSourcedEntitySubscription(clazz).map { _ =>
      val entityType = findEventSourcedEntityType(clazz)
      val in = EventSource.newBuilder().setEventSourcedEntity(entityType)
      val eventing = ServiceEventing.newBuilder().setIn(in)
      kalix.ServiceOptions.newBuilder().setEventing(eventing).build()
    }
  }

  def eventingInForTopicServiceLevel(clazz: Class[_]): Option[kalix.ServiceOptions] = {
    topicSubscription(clazz).map { ann =>
      val in = EventSource.newBuilder().setTopic(ann.value()).setConsumerGroup(ann.consumerGroup())
      val eventing = ServiceEventing.newBuilder().setIn(in)
      kalix.ServiceOptions.newBuilder().setEventing(eventing).build()
    }
  }

  def topicEventSource(javaMethod: Method): EventSource = {
    val topicName = findSubscriptionTopicName(javaMethod)
    val consumerGroup = findSubscriptionConsumerGroup(javaMethod)
    EventSource.newBuilder().setTopic(topicName).setConsumerGroup(consumerGroup).build()
  }

  def topicEventSource(clazz: Class[_]): EventSource = {
    val topicName = findSubscriptionTopicName(clazz)
    val consumerGroup = findSubscriptionConsumerGroup(clazz)
    EventSource.newBuilder().setTopic(topicName).setConsumerGroup(consumerGroup).build()
  }

  def eventingOutForTopic(javaMethod: Method): Option[Eventing] = {
    topicEventDestination(javaMethod).map(eventSource => Eventing.newBuilder().setOut(eventSource).build())
  }

  def eventingInForValueEntity(entityType: String, handleDeletes: Boolean): Eventing = {
    val eventSource = EventSource
      .newBuilder()
      .setValueEntity(entityType)
      .setHandleDeletes(handleDeletes)
      .build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def subscribeToEventStream(component: Class[_]): Option[kalix.ServiceOptions] = {
    Option(component.getAnnotation(classOf[Subscribe.Stream])).map { streamAnn =>
      val direct = DirectSource
        .newBuilder()
        .setEventStreamId(streamAnn.id())
        .setService(streamAnn.service())

      val in = EventSource
        .newBuilder()
        .setDirect(direct)
        .setConsumerGroup(streamAnn.consumerGroup())

      val eventing =
        ServiceEventing
          .newBuilder()
          .setIn(in)

      kalix.ServiceOptions
        .newBuilder()
        .setEventing(eventing)
        .build()
    }
  }

  def publishToEventStream(component: Class[_]): Option[kalix.ServiceOptions] = {
    Option(component.getAnnotation(classOf[Publish.Stream])).map { streamAnn =>

      val direct = DirectDestination
        .newBuilder()
        .setEventStreamId(streamAnn.id())

      val out = ServiceEventingOut
        .newBuilder()
        .setDirect(direct)

      val eventing =
        ServiceEventing
          .newBuilder()
          .setOut(out)

      kalix.ServiceOptions
        .newBuilder()
        .setEventing(eventing)
        .build()
    }
  }

  // TODO: add more validations here
  // we should let users know if components are missing required annotations,
  // eg: Workflow and Entities require @TypeId, View requires @Table and @Subscription
  def getFactoryFor(component: Class[_]): ComponentDescriptorFactory = {
    if (component.getAnnotation(classOf[TypeId]) != null || component.getAnnotation(classOf[EntityType]) != null)
      EntityDescriptorFactory
    else if (component.getAnnotation(classOf[Table]) != null || component.getAnnotation(classOf[ViewId]) != null)
      ViewDescriptorFactory
    else
      ActionDescriptorFactory
  }

  def combineByES(
      subscriptions: Seq[KalixMethod],
      messageCodec: JsonMessageCodec,
      component: Class[_]): Seq[KalixMethod] = {

    def groupByES(methods: Seq[KalixMethod]): Map[String, Seq[KalixMethod]] = {
      val withEventSourcedIn = methods.filter(kalixMethod =>
        kalixMethod.methodOptions.exists(option =>
          option.hasEventing && option.getEventing.hasIn && option.getEventing.getIn.hasEventSourcedEntity))
      //Assuming there is only one eventing.in annotation per method, therefore head is as good as any other
      withEventSourcedIn.groupBy(m => m.methodOptions.head.getEventing.getIn.getEventSourcedEntity)
    }

    combineBy("ES", groupByES(subscriptions), messageCodec, component)
  }

  def combineByTopic(
      kalixMethods: Seq[KalixMethod],
      messageCodec: JsonMessageCodec,
      component: Class[_]): Seq[KalixMethod] = {
    def groupByTopic(methods: Seq[KalixMethod]): Map[String, Seq[KalixMethod]] = {
      val withTopicIn = methods.filter(kalixMethod =>
        kalixMethod.methodOptions.exists(option =>
          option.hasEventing && option.getEventing.hasIn && option.getEventing.getIn.hasTopic))
      //Assuming there is only one topic annotation per method, therefore head is as good as any other
      withTopicIn.groupBy(m => m.methodOptions.head.getEventing.getIn.getTopic)
    }

    combineBy("Topic", groupByTopic(kalixMethods), messageCodec, component)
  }

  def combineBy(
      sourceName: String,
      groupedSubscriptions: Map[String, Seq[KalixMethod]],
      messageCodec: JsonMessageCodec,
      component: Class[_]): Seq[KalixMethod] = {

    groupedSubscriptions.collect {
      case (source, kMethods) if kMethods.size > 1 =>
        val methodsMap =
          kMethods.flatMap { k =>
            val methodParameterTypes = k.serviceMethod.javaMethodOpt.get.getParameterTypes
            // it is safe to pick the last parameter. An action has one and View has two. In the View always the last is the event
            val eventParameter = methodParameterTypes.last

            messageCodec.typeUrlsFor(eventParameter).map(typeUrl => (typeUrl, k.serviceMethod.javaMethodOpt.get))
          }.toMap

        KalixMethod(
          CombinedSubscriptionServiceMethod(
            component.getName,
            "KalixSyntheticMethodOn" + sourceName + escapeMethodName(source.capitalize),
            methodsMap))
          .withKalixOptions(kMethods.head.methodOptions)

      case (_, kMethod +: Nil) => kMethod
    }.toSeq
  }

  private[impl] def escapeMethodName(value: String): String = {
    value.replaceAll("[\\._\\-]", "")
  }

  private[impl] def buildEventingOutOptions(method: Method): Option[MethodOptions] =
    eventingOutForTopic(method)
      .map(eventingOut => kalix.MethodOptions.newBuilder().setEventing(eventingOut).build())

  def mergeServiceOptions(allOptions: Option[kalix.ServiceOptions]*): Option[ServiceOptions] = {
    val mergedOptions =
      allOptions.flatten
        .foldLeft(kalix.ServiceOptions.newBuilder()) { case (builder, serviceOptions) =>
          builder.mergeFrom(serviceOptions)
        }
        .build()

    // if builder produces the default one, we can returns a None
    if (mergedOptions == kalix.ServiceOptions.getDefaultInstance) None
    else Some(mergedOptions)
  }
}

private[impl] trait ComponentDescriptorFactory {

  /**
   * Inspect the component class (type), validate the annotations/methods and build a component descriptor for it.
   */
  def buildDescriptorFor(
      componentClass: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor

}

/**
 * Thrown when the component has incorrect annotations
 */
final case class InvalidComponentException(message: String) extends RuntimeException(message)
