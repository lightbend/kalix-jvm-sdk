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

import kalix.springsdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.springsdk.impl.ComponentDescriptorFactory.combineByES
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntityServiceLevel
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForTopic
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingOutForTopic
import kalix.springsdk.impl.ComponentDescriptorFactory.findEventSourcedEntityType
import kalix.springsdk.impl.ComponentDescriptorFactory.hasActionOutput
import kalix.springsdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasTopicPublication
import kalix.springsdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.publishToEventStream
import kalix.springsdk.impl.ComponentDescriptorFactory.subscribeToEventStream
import kalix.springsdk.impl.ComponentDescriptorFactory.validateRestMethod
import kalix.springsdk.impl.reflection.CombinedSubscriptionServiceMethod
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.SubscriptionServiceMethod
import kalix.springsdk.impl.reflection.ReflectionUtils
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private[impl] object ActionDescriptorFactory extends ComponentDescriptorFactory {

  private val logger: Logger = LoggerFactory.getLogger(classOf[ActionDescriptorFactory.type])

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    // TODO: we need more robust validation covering all the corner cases
    // verify component is correctly configured
    if (hasEventSourcedEntitySubscription(component) &&
      component.getMethods.exists(hasEventSourcedEntitySubscription))
      throw InvalidComponentException(
        "You cannot use @Subscribe.EventSourcedEntity annotation in " +
        "methods and class. You can do either one or the other.")

    if (hasTopicSubscription(component) && component.getMethods.exists(hasTopicSubscription))
      throw InvalidComponentException(
        "You cannot use @Subscribe.Topic annotation in " +
        "both methods and class. You can do either one or the other.")

    //we should merge from here
    // methods with REST annotations
    val syntheticMethods: Seq[KalixMethod] =
      RestServiceIntrospector.inspectService(component).methods.map { serviceMethod =>
        validateRestMethod(serviceMethod.javaMethod)
        KalixMethod(serviceMethod).withKalixOptions(buildJWTOptions(serviceMethod.javaMethod))
      }

    //TODO make sure no subscription should be exposed via REST.
    // methods annotated with @Subscribe.ValueEntity
    import ReflectionUtils.methodOrdering
    val subscriptionValueEntityMethods: IndexedSeq[KalixMethod] = component.getMethods
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
              "KalixSyntheticMethodOnTopic" + topic.capitalize.replaceAll("[\\._\\-]", ""),
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

    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = serviceLevelOptions,
      component.getPackageName,
      addKalixOptions(syntheticMethods, publicationTopicMethods)
      ++ subscriptionValueEntityMethods
      ++ combineByES(subscriptionEventSourcedEntityMethods, messageCodec, component)
      ++ combineByES(subscriptionEventSourcedEntityClass, messageCodec, component)
      ++ combineByTopic(subscriptionTopicClass)
      ++ combineByTopic(subscriptionTopicMethods)
      ++ removeDuplicates(syntheticMethods, publicationTopicMethods))
  }
}
