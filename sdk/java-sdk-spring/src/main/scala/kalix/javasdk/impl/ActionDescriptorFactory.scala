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

import kalix.javasdk.impl
import kalix.javasdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.javasdk.impl.ComponentDescriptorFactory.combineByES
import kalix.javasdk.impl.ComponentDescriptorFactory.escapeMethodName
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntityServiceLevel
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingOutForTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.findEventSourcedEntityType
import kalix.javasdk.impl.ComponentDescriptorFactory.hasActionOutput
import kalix.javasdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.javasdk.impl.ComponentDescriptorFactory.hasTopicPublication
import kalix.javasdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.publishToEventStream
import kalix.javasdk.impl.ComponentDescriptorFactory.streamSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.subscribeToEventStream
import kalix.javasdk.impl.reflection.CombinedSubscriptionServiceMethod
import kalix.javasdk.impl.reflection.HandleDeletesServiceMethod
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.impl.reflection.ReflectionUtils
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.SubscriptionServiceMethod

private[impl] object ActionDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    // we should merge from here
    // methods with REST annotations
    val syntheticMethods: Seq[KalixMethod] =
      RestServiceIntrospector.inspectService(component).methods.map { serviceMethod =>
        KalixMethod(serviceMethod).withKalixOptions(buildJWTOptions(serviceMethod.javaMethod))
      }

    //TODO make sure no subscription should be exposed via REST.
    // methods annotated with @Subscribe.ValueEntity
    import ReflectionUtils.methodOrdering
    val handleDeletesMethods = component.getMethods
      .filter(hasHandleDeletes)
      .sorted
      .map { method =>
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
        KalixMethod(HandleDeletesServiceMethod(method))
          .withKalixOptions(methodOptionsBuilder.build())
      }

    val subscriptionValueEntityMethods: IndexedSeq[KalixMethod] = component.getMethods
      .filterNot(hasHandleDeletes)
      .filter(hasValueEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val subscriptionOptions = eventingInForValueEntity(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
      .toIndexedSeq

    // methods annotated with @Subscribe.EventSourcedEntity
    val subscriptionEventSourcedEntityMethods: IndexedSeq[KalixMethod] = component.getMethods
      .filter(hasEventSourcedEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val subscriptionOptions = eventingInForEventSourcedEntity(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()
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
            .map { method => KalixMethod(SubscriptionServiceMethod(method)) }
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
              .map { method => KalixMethod(SubscriptionServiceMethod(method)) }
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
        val subscriptionOptions = eventingInForTopic(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
      .toIndexedSeq

    // type level @Subscribe.Topic, methods eligible for subscription
    val subscriptionTopicClass: IndexedSeq[KalixMethod] =
      if (hasTopicSubscription(component)) {
        component.getMethods
          .filter(hasActionOutput)
          .sorted // make sure we get the methods in deterministic order
          .map { method =>
            val subscriptionOptions = eventingInForTopic(component)
            val kalixOptions =
              kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

            KalixMethod(SubscriptionServiceMethod(method))
              .withKalixOptions(kalixOptions)
          }
          .toIndexedSeq
      } else IndexedSeq.empty

    def combineByTopic(kalixMethods: Seq[KalixMethod]): Seq[KalixMethod] = {
      def groupByTopic(methods: Seq[KalixMethod]): Map[String, Seq[KalixMethod]] = {
        val withTopicIn = methods.filter(kalixMethod =>
          kalixMethod.methodOptions.exists(option =>
            option.hasEventing && option.getEventing.hasIn && option.getEventing.getIn.hasTopic))
        //Assuming there is only one topic annotation per method, therefore head is as good as any other
        withTopicIn.groupBy(m => m.methodOptions.head.getEventing.getIn.getTopic)
      }

      groupByTopic(kalixMethods).collect {
        case (topic, kMethods) if kMethods.size > 1 =>
          val methodsMap =
            kMethods.map { k =>
              val inputType = k.serviceMethod.javaMethodOpt.get.getParameterTypes.head
              val typeUrl = messageCodec.typeUrlFor(inputType)
              (typeUrl, k.serviceMethod.javaMethodOpt.get)
            }.toMap
          KalixMethod(
            CombinedSubscriptionServiceMethod(
              component.getName,
              "KalixSyntheticMethodOnTopic" + escapeMethodName(topic.capitalize),
              methodsMap))
            .withKalixOptions(kMethods.head.methodOptions)
        case (_, kMethod +: Nil) =>
          kMethod
      }.toSeq
    }

    // methods annotated with @Publish.Topic
    val publicationTopicMethods: IndexedSeq[KalixMethod] = component.getMethods
      .filter(hasTopicPublication)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val publicationOptions = eventingOutForTopic(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(publicationOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
      .toIndexedSeq

    val serviceName = nameGenerator.getName(component.getSimpleName)

    def addKalixOptions(to: Seq[KalixMethod], from: Seq[KalixMethod]): Seq[KalixMethod] = {
      val added = to.flatMap(toAdd =>
        from
          .filter { addingFrom =>
            addingFrom.serviceMethod.methodName.equals(toAdd.serviceMethod.methodName)
          }
          .map(addingFrom => toAdd.withKalixOptions(addingFrom.methodOptions)))
      val unmatchedInTo = to
        .filter { toAdd =>
          !from.exists { addingFrom =>
            addingFrom.serviceMethod.methodName.equals(toAdd.serviceMethod.methodName)
          }
        }
      added ++ unmatchedInTo
    }

    def removeDuplicates(springMethods: Seq[KalixMethod], pubSubMethods: Seq[KalixMethod]): Seq[KalixMethod] = {
      pubSubMethods.filterNot(p =>
        springMethods.exists(s => p.serviceMethod.methodName.equals(s.serviceMethod.methodName)))
    }

    val serviceLevelOptions = {

      val allOptions =
        AclDescriptorFactory.serviceLevelAclAnnotation(component) ::
        eventingInForEventSourcedEntityServiceLevel(component) ::
        subscribeToEventStream(component) ::
        publishToEventStream(component) :: Nil

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

    impl.ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = serviceLevelOptions,
      component.getPackageName,
      addKalixOptions(syntheticMethods, publicationTopicMethods)
      ++ handleDeletesMethods
      ++ subscriptionValueEntityMethods
      ++ combineByES(subscriptionEventSourcedEntityMethods, messageCodec, component)
      ++ combineByES(subscriptionEventSourcedEntityClass, messageCodec, component)
      ++ combineByES(subscriptionStreamClass, messageCodec, component)
      ++ combineByTopic(subscriptionTopicClass)
      ++ combineByTopic(subscriptionTopicMethods)
      ++ removeDuplicates(syntheticMethods, publicationTopicMethods))
  }
}
