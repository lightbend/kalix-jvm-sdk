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
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForTopic
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingOutForTopic
import kalix.springsdk.impl.ComponentDescriptorFactory.hasActionOutput
import kalix.springsdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasTopicPublication
import kalix.springsdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.validateRestMethod
import kalix.springsdk.impl.reflection.CombinedSubscriptionServiceMethod
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.ReflectionUtils
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.SubscriptionServiceMethod

private[impl] object ActionDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {
    //we should merge from here
    val syntheticMethods =
      RestServiceIntrospector.inspectService(component).methods.map { serviceMethod =>
        validateRestMethod(serviceMethod.javaMethod)
        KalixMethod(serviceMethod).withKalixOptions(buildJWTOptions(serviceMethod.javaMethod))
      }

    //TODO make sure no subscription should be exposed via REST.
    import ReflectionUtils.methodOrdering
    val subscriptionValueEntityMethods = component.getMethods
      .filter(hasValueEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val subscriptionOptions = eventingInForValueEntity(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }

    val subscriptionEventSourcedEntityMethods = component.getMethods
      .filter(hasEventSourcedEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        if (hasEventSourcedEntitySubscription(component))
          throw InvalidComponentException(
            s"You cannot use Subscribe annotation in both the methods and the class. You can do either one or the other.")
        val subscriptionOptions = eventingInForEventSourcedEntity(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }

    val subscriptionEventSourcedEntityClass =
      if (hasEventSourcedEntitySubscription(component)) {
        component.getMethods.sorted // make sure we get the methods in deterministic order
          .filter(hasActionOutput)
          .collect { case method =>
            val subscriptionOptions = eventingInForEventSourcedEntity(component)
            val kalixOptions =
              kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

            KalixMethod(SubscriptionServiceMethod(method))
              .withKalixOptions(kalixOptions)
          }
      } else Array.empty[KalixMethod]

    val subscriptionTopicMethods = component.getMethods
      .filter(hasTopicSubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        if (hasTopicSubscription(component))
          throw InvalidComponentException(
            s"You cannot use Subscribe annotation in both the methods and the class. You can do either one or the other.")
        val subscriptionOptions = eventingInForTopic(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }

    val subscriptionTopicClass: Array[KalixMethod] =
      if (hasTopicSubscription(component)) {
        component.getMethods.sorted // make sure we get the methods in deterministic order
          .filter(hasActionOutput)
          .collect { case method =>
            val subscriptionOptions = eventingInForTopic(component)
            val kalixOptions =
              kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

            KalixMethod(SubscriptionServiceMethod(method))
              .withKalixOptions(kalixOptions)
          }
      } else Array.empty[KalixMethod]

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

    // TODO: we need to revisit this. A Publish should not be a Subscription
    val publicationTopicMethods = component.getMethods
      .filter(hasTopicPublication)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        val publicationOptions = eventingOutForTopic(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(publicationOptions).build()

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }
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

    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = AclDescriptorFactory.serviceLevelAclAnnotation(component),
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
