/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.EventSource
import kalix.Eventing
import kalix.MethodOptions
import kalix.javasdk.impl
import kalix.javasdk.impl.ComponentDescriptorFactory.buildEventingOutOptions
import kalix.javasdk.impl.ComponentDescriptorFactory.combineBy
import kalix.javasdk.impl.ComponentDescriptorFactory.combineByES
import kalix.javasdk.impl.ComponentDescriptorFactory.combineByTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.eventSourceEntityEventSource
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntityServiceLevel
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForValueEntityServiceLevel
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingOutForTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.findEventSourcedEntityType
import kalix.javasdk.impl.ComponentDescriptorFactory.findSubscriptionTopicName
import kalix.javasdk.impl.ComponentDescriptorFactory.hasActionOutput
import kalix.javasdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.javasdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.publishToEventStream
import kalix.javasdk.impl.ComponentDescriptorFactory.streamSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.subscribeToEventStream
import kalix.javasdk.impl.ComponentDescriptorFactory.topicEventDestination
import kalix.javasdk.impl.ComponentDescriptorFactory.topicEventSource
import kalix.javasdk.impl.ComponentDescriptorFactory.valueEntityEventSource
import kalix.javasdk.impl.reflection.HandleDeletesServiceMethod
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.SubscriptionServiceMethod
import kalix.javasdk.impl.reflection.Reflect.Syntax.AnnotatedElementOps
import kalix.javasdk.impl.reflection.Reflect.Syntax.MethodOps
import kalix.javasdk.impl.reflection.Reflect
import java.lang.reflect.Method

import kalix.javasdk.impl.ComponentDescriptorFactory.mergeServiceOptions
import kalix.TriggerOptions
import kalix.javasdk.annotations.Trigger

private[impl] object ActionDescriptorFactory extends ComponentDescriptorFactory {

  private def hasTriggerMethodOptions(javaMethod: Method): Boolean = {
    javaMethod.isPublic && javaMethod.hasAnnotation[Trigger.OnStartup] // this is the only event available at the moment
  }

  private def triggerOptions(javaMethod: Method): Option[TriggerOptions] = {
    Option.when(hasTriggerMethodOptions(javaMethod)) {
      val ann = javaMethod.getAnnotation(classOf[Trigger.OnStartup]);

      TriggerOptions
        .newBuilder()
        .setOn(TriggerOptions.TriggerEvent.STARTUP) // this is the only event available at the moment
        .setMaxRetries(ann.maxRetries())
        .build()
    }
  }

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    def withOptionalDestination(method: Method, source: EventSource): MethodOptions = {
      val eventingBuilder = Eventing.newBuilder().setIn(source)
      topicEventDestination(method).foreach(eventingBuilder.setOut)
      kalix.MethodOptions.newBuilder().setEventing(eventingBuilder.build()).build()
    }

    // we should merge from here
    // methods with REST annotations
    val syntheticMethods: Seq[KalixMethod] =
      RestServiceIntrospector.inspectService(component).methods.map { serviceMethod =>
        val optionsBuilder = kalix.MethodOptions.newBuilder()
        eventingOutForTopic(serviceMethod.javaMethod).foreach(optionsBuilder.setEventing)
        JwtDescriptorFactory.jwtOptions(serviceMethod.javaMethod).foreach(optionsBuilder.setJwt)
        triggerOptions(serviceMethod.javaMethod).foreach(optionsBuilder.setTrigger)
        KalixMethod(serviceMethod).withKalixOptions(optionsBuilder.build())
      }

    //TODO make sure no subscription should be exposed via REST.
    // methods annotated with @Subscribe.ValueEntity
    import Reflect.methodOrdering

    val handleDeletesMethods = component.getMethods
      .filter(hasHandleDeletes)
      .sorted
      .map { method =>
        val source = valueEntityEventSource(method)
        val kalixOptions = withOptionalDestination(method, source)
        KalixMethod(HandleDeletesServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }

    val subscriptionValueEntityMethods: IndexedSeq[KalixMethod] = if (hasValueEntitySubscription(component)) {
      //expecting only a single update method, which is validated
      component.getMethods
        .filter(hasActionOutput)
        .map { method =>
          KalixMethod(SubscriptionServiceMethod(method))
            .withKalixOptions(buildEventingOutOptions(method))
        }
        .toIndexedSeq
    } else {
      component.getMethods
        .filterNot(hasHandleDeletes)
        .filter(hasValueEntitySubscription)
        .sorted // make sure we get the methods in deterministic order
        .map { method =>
          val source = valueEntityEventSource(method)
          val kalixOptions = withOptionalDestination(method, source)
          KalixMethod(SubscriptionServiceMethod(method))
            .withKalixOptions(kalixOptions)
        }
        .toIndexedSeq
    }

    // methods annotated with @Subscribe.EventSourcedEntity
    val subscriptionEventSourcedEntityMethods: IndexedSeq[KalixMethod] = component.getMethods
      .filter(hasEventSourcedEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val source = eventSourceEntityEventSource(method)
        val kalixOptions = withOptionalDestination(method, source)
        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
      .toIndexedSeq

    val subscriptionEventSourcedEntityClass: Map[String, Seq[KalixMethod]] =
      if (hasEventSourcedEntitySubscription(component)) {
        val kalixMethods =
          component.getMethods
            .filter(hasActionOutput)
            .sorted // make sure we get the methods in deterministic order
            .map { method =>
              KalixMethod(SubscriptionServiceMethod(method))
                .withKalixOptions(buildEventingOutOptions(method))
            }
            .toSeq

        val entityType = findEventSourcedEntityType(component)
        Map(entityType -> kalixMethods)

      } else Map.empty

    val subscriptionStreamClass: Map[String, Seq[KalixMethod]] = {
      streamSubscription(component)
        .map { ann =>
          val kalixMethods =
            component.getMethods
              .filter(hasActionOutput)
              .sorted // make sure we get the methods in deterministic order
              .map { method =>
                KalixMethod(SubscriptionServiceMethod(method))
                  .withKalixOptions(buildEventingOutOptions(method))
              }
              .toSeq

          val streamId = ann.id()
          Map(streamId -> kalixMethods)
        }
        .getOrElse(Map.empty)
    }

    // methods annotated with @Subscribe.Topic
    val subscriptionTopicMethods: IndexedSeq[KalixMethod] = component.getMethods
      .filter(hasTopicSubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val source = topicEventSource(method)
        val kalixOptions = withOptionalDestination(method, source)
        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
      .toIndexedSeq

    // type level @Subscribe.Topic, methods eligible for subscription
    val subscriptionTopicClass: Map[String, Seq[KalixMethod]] =
      if (hasTopicSubscription(component)) {
        val kalixMethods = component.getMethods
          .filter(hasActionOutput)
          .sorted // make sure we get the methods in deterministic order
          .map { method =>
            val source = topicEventSource(component)
            val kalixOptions = withOptionalDestination(method, source)
            KalixMethod(SubscriptionServiceMethod(method))
              .withKalixOptions(kalixOptions)
          }
          .toIndexedSeq
        val topicName = findSubscriptionTopicName(component)
        Map(topicName -> kalixMethods)
      } else Map.empty

    val serviceName = nameGenerator.getName(component.getSimpleName)

    val serviceLevelOptions =
      mergeServiceOptions(
        AclDescriptorFactory.serviceLevelAclAnnotation(component),
        JwtDescriptorFactory.serviceLevelJwtAnnotation(component),
        eventingInForEventSourcedEntityServiceLevel(component),
        eventingInForValueEntityServiceLevel(component),
        subscribeToEventStream(component),
        publishToEventStream(component))

    impl.ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = serviceLevelOptions,
      component.getPackageName,
      syntheticMethods
      ++ handleDeletesMethods
      ++ subscriptionValueEntityMethods
      ++ combineByES(subscriptionEventSourcedEntityMethods, messageCodec, component)
      ++ combineByTopic(subscriptionTopicMethods, messageCodec, component)
      ++ combineBy("ES", subscriptionEventSourcedEntityClass, messageCodec, component)
      ++ combineBy("Stream", subscriptionStreamClass, messageCodec, component)
      ++ combineBy("Topic", subscriptionTopicClass, messageCodec, component))
  }
}
