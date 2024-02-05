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

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import kalix.Eventing
import kalix.MethodOptions
import kalix.javasdk.annotations.Query
import kalix.javasdk.annotations.Subscribe
import kalix.javasdk.annotations.Table
import kalix.javasdk.impl.JwtDescriptorFactory.buildJWTOptions
import kalix.javasdk.impl.ComponentDescriptorFactory.combineBy
import kalix.javasdk.impl.ComponentDescriptorFactory.combineByES
import kalix.javasdk.impl.ComponentDescriptorFactory.combineByTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntityServiceLevel
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForTopic
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForTopicServiceLevel
import kalix.javasdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.javasdk.impl.ComponentDescriptorFactory.findEventSourcedEntityType
import kalix.javasdk.impl.ComponentDescriptorFactory.findHandleDeletes
import kalix.javasdk.impl.ComponentDescriptorFactory.findSubscriptionTopicName
import kalix.javasdk.impl.ComponentDescriptorFactory.findValueEntityType
import kalix.javasdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.javasdk.impl.ComponentDescriptorFactory.hasStreamSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasTopicSubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.hasUpdateEffectOutput
import kalix.javasdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.javasdk.impl.ComponentDescriptorFactory.mergeServiceOptions
import kalix.javasdk.impl.ComponentDescriptorFactory.subscribeToEventStream
import kalix.javasdk.impl.reflection.HandleDeletesServiceMethod
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.impl.reflection.ReflectionUtils
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.javasdk.impl.reflection.SubscriptionServiceMethod
import kalix.javasdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.javasdk.impl.reflection.VirtualDeleteServiceMethod
import kalix.javasdk.impl.reflection.VirtualServiceMethod
import kalix.spring.impl.KalixSpringApplication
// TODO: abstract away reactor dependency
import reactor.core.publisher.Flux

private[impl] object ViewDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    val isMultiTable = KalixSpringApplication.isMultiTableView(component)

    val tableComponents =
      if (isMultiTable) component.getDeclaredClasses.toSeq.filter(KalixSpringApplication.isNestedViewTable)
      else Seq(component)

    val (tableTypeDescriptors, updateMethods) = {
      tableComponents
        .map { component =>
          // View class type parameter declares table type
          val tableType: Class[_] =
            component.getGenericSuperclass
              .asInstanceOf[ParameterizedType]
              .getActualTypeArguments
              .head
              .asInstanceOf[Class[_]]

          val tableName: String = component.getAnnotation(classOf[Table]).value()
          val tableTypeDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(tableType)

          val tableProtoMessageName = tableTypeDescriptor.mainMessageDescriptor.getName

          val hasMethodLevelEventSourcedEntitySubs = component.getMethods.exists(hasEventSourcedEntitySubscription)
          val hasTypeLevelEventSourcedEntitySubs = hasEventSourcedEntitySubscription(component)
          val hasTypeLevelValueEntitySubs = hasValueEntitySubscription(component)
          val hasMethodLevelValueEntitySubs = component.getMethods.exists(hasValueEntitySubscription)
          val hasTypeLevelTopicSubs = hasTopicSubscription(component)
          val hasMethodLevelTopicSubs = component.getMethods.exists(hasTopicSubscription)
          val hasTypeLevelStreamSubs = hasStreamSubscription(component)

          val updateMethods = {
            if (hasTypeLevelValueEntitySubs)
              subscriptionForTypeLevelValueEntity(component, tableType, tableName, tableProtoMessageName)
            else if (hasMethodLevelValueEntitySubs)
              subscriptionForMethodLevelValueEntity(component, tableName, tableProtoMessageName)
            else if (hasTypeLevelEventSourcedEntitySubs) {
              val kalixSubscriptionMethods =
                methodsForTypeLevelESSubscriptions(component, tableName, tableProtoMessageName, isMultiTable)
              combineBy("ES", kalixSubscriptionMethods, messageCodec, component)
            } else if (hasMethodLevelEventSourcedEntitySubs) {
              val methodsForMethodLevelESSubscriptions =
                subscriptionEventSourcedEntityMethodLevel(component, tableName, tableProtoMessageName)
              combineByES(methodsForMethodLevelESSubscriptions, messageCodec, component)
            } else if (hasTypeLevelTopicSubs) {
              val kalixSubscriptionMethods =
                methodsForTypeLevelTopicSubscriptions(component, tableName, tableProtoMessageName, isMultiTable)
              combineBy("Topic", kalixSubscriptionMethods, messageCodec, component)
            } else if (hasMethodLevelTopicSubs) {
              val methodsForMethodLevelTopicSubscriptions =
                subscriptionTopicMethodLevel(component, tableName, tableProtoMessageName)
              combineByTopic(methodsForMethodLevelTopicSubscriptions, messageCodec, component)
            } else if (hasTypeLevelStreamSubs) {
              val kalixSubscriptionMethods =
                methodsForTypeLevelStreamSubscriptions(component, tableName, tableProtoMessageName)
              combineBy("Stream", kalixSubscriptionMethods, messageCodec, component)
            } else
              Seq.empty
          }

          tableTypeDescriptor -> updateMethods
        }
        .foldLeft((Seq.empty[ProtoMessageDescriptors], Seq.empty[KalixMethod])) {
          case ((tableTypeDescriptors, allUpdateMethods), (tableTypeDescriptor, updateMethods)) =>
            (tableTypeDescriptors :+ tableTypeDescriptor, allUpdateMethods ++ updateMethods)
        }
    }

    val allQueryMethods = queryMethods(component)

    val kalixMethods: Seq[KalixMethod] = allQueryMethods.map(_.queryMethod) ++ updateMethods
    val serviceName = nameGenerator.getName(component.getSimpleName)
    val additionalMessages =
      tableTypeDescriptors.toSet ++ allQueryMethods.map(_.queryOutputSchemaDescriptor) ++ allQueryMethods.flatMap(
        _.queryInputSchemaDescriptor.toSet)

    val serviceLevelOptions =
      mergeServiceOptions(
        AclDescriptorFactory.serviceLevelAclAnnotation(component),
        JwtDescriptorFactory.serviceLevelJwtAnnotation(component),
        eventingInForEventSourcedEntityServiceLevel(component),
        eventingInForTopicServiceLevel(component),
        subscribeToEventStream(component))

    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = serviceLevelOptions,
      component.getPackageName,
      kalixMethods,
      additionalMessages.toSeq)
  }

  private case class QueryMethod(
      queryMethod: KalixMethod,
      queryInputSchemaDescriptor: Option[ProtoMessageDescriptors],
      queryOutputSchemaDescriptor: ProtoMessageDescriptors)

  private def queryMethods(component: Class[_]): Seq[QueryMethod] = {
    // we only take methods with Query annotations and Spring REST annotations
    val annotatedQueryMethods = RestServiceIntrospector
      .inspectService(component)
      .methods
      .filter(_.javaMethod.getAnnotation(classOf[Query]) != null)

    annotatedQueryMethods.map { queryMethod =>
      val queryOutputType = {
        val returnType = queryMethod.javaMethod.getReturnType
        if (returnType == classOf[Flux[_]]) {
          queryMethod.javaMethod.getGenericReturnType
            .asInstanceOf[ParameterizedType] // Flux will be a ParameterizedType
            .getActualTypeArguments
            .head // only one type parameter, safe to pick the head
            .asInstanceOf[Class[_]]
        } else returnType
      }

      val queryOutputSchemaDescriptor =
        ProtoMessageDescriptors.generateMessageDescriptors(queryOutputType)

      val queryInputSchemaDescriptor =
        queryMethod.params.find(_.isInstanceOf[BodyParameter]).map { case BodyParameter(param, _) =>
          ProtoMessageDescriptors.generateMessageDescriptors(param.getParameterType)
        }

      val queryAnnotation = queryMethod.javaMethod.getAnnotation(classOf[Query])
      val queryStr = queryAnnotation.value()

      val query = kalix.View.Query
        .newBuilder()
        .setQuery(queryStr)
        .setStreamUpdates(queryAnnotation.streamUpdates())
        .build()

      val jsonSchema = {
        val builder = kalix.JsonSchema
          .newBuilder()
          .setOutput(queryOutputSchemaDescriptor.mainMessageDescriptor.getName)

        queryInputSchemaDescriptor.foreach { inputSchema =>
          builder
            .setInput(inputSchema.mainMessageDescriptor.getName)
            .setJsonBodyInputField("json_body")

        }
        builder.build()
      }

      val view = kalix.View
        .newBuilder()
        .setJsonSchema(jsonSchema)
        .setQuery(query)
        .build()

      val builder = kalix.MethodOptions.newBuilder()
      builder.setView(view)
      val methodOptions = builder.build()

      // since it is a query, we don't actually ever want to handle any request in the SDK
      // the proxy does the work for us, mark the method as non-callable
      val kalixQueryMethod = KalixMethod(queryMethod.copy(callable = false), methodOptions = Some(methodOptions))
        .withKalixOptions(buildJWTOptions(queryMethod.javaMethod))

      QueryMethod(kalixQueryMethod, queryInputSchemaDescriptor, queryOutputSchemaDescriptor)
    }
  }

  private def methodsForTypeLevelStreamSubscriptions(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String): Map[String, Seq[KalixMethod]] = {
    val methods = eligibleSubscriptionMethods(component, tableName, tableProtoMessageName, None).toIndexedSeq
    val ann = component.getAnnotation(classOf[Subscribe.Stream])
    val key = ann.id().capitalize
    Map(key -> methods)
  }

  private def methodsForTypeLevelESSubscriptions(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String,
      isMultiTable: Boolean): Map[String, Seq[KalixMethod]] = {

    val methods = eligibleSubscriptionMethods(
      component,
      tableName,
      tableProtoMessageName,
      if (isMultiTable) Some(eventingInForEventSourcedEntity(component)) else None).toIndexedSeq
    val entityType = findEventSourcedEntityType(component)
    Map(entityType -> methods)
  }

  private def methodsForTypeLevelTopicSubscriptions(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String,
      isMultiTable: Boolean): Map[String, Seq[KalixMethod]] = {

    val methods = eligibleSubscriptionMethods(
      component,
      tableName,
      tableProtoMessageName,
      if (isMultiTable) Some(eventingInForTopic(component)) else None).toIndexedSeq
    val entityType = findSubscriptionTopicName(component)
    Map(entityType -> methods)
  }

  private def eligibleSubscriptionMethods(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String,
      eventing: Option[Eventing]) =
    component.getMethods.filter(hasUpdateEffectOutput).map { method =>
      // event sourced or topic subscription updates
      val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

      eventing.foreach(methodOptionsBuilder.setEventing)

      addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

      KalixMethod(SubscriptionServiceMethod(method))
        .withKalixOptions(methodOptionsBuilder.build())
    }

  private def subscriptionEventSourcedEntityMethodLevel(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {

    def getMethodsWithSubscription(component: Class[_]): Seq[Method] = {
      import ReflectionUtils.methodOrdering
      component.getMethods
        .filter(hasEventSourcedEntitySubscription)
        .sorted
        .toIndexedSeq
    }

    def getEventing(method: Method, component: Class[_]): Eventing =
      if (hasEventSourcedEntitySubscription(component)) eventingInForEventSourcedEntity(component)
      else eventingInForEventSourcedEntity(method)

    getMethodsWithSubscription(component).map { method =>
      // event sourced or topic subscription updates
      val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

      if (hasEventSourcedEntitySubscription(method))
        methodOptionsBuilder.setEventing(getEventing(method, component))

      addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

      KalixMethod(SubscriptionServiceMethod(method))
        .withKalixOptions(methodOptionsBuilder.build())
    }
  }

  private def subscriptionTopicMethodLevel(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {

    def getMethodsWithSubscription(component: Class[_]): Seq[Method] = {
      import ReflectionUtils.methodOrdering
      component.getMethods
        .filter(hasTopicSubscription)
        .sorted
        .toIndexedSeq
    }

    getMethodsWithSubscription(component).map { method =>
      // event sourced or topic subscription updates
      val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

      methodOptionsBuilder.setEventing(eventingInForTopic(method))

      addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

      KalixMethod(SubscriptionServiceMethod(method))
        .withKalixOptions(methodOptionsBuilder.build())
    }
  }

  private def subscriptionForMethodLevelValueEntity(
      component: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {

    import ReflectionUtils.methodOrdering

    val handleDeletesMethods = component.getMethods
      .filter(hasHandleDeletes)
      .sorted
      .map { method =>
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
        addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, transform = true)

        KalixMethod(HandleDeletesServiceMethod(method))
          .withKalixOptions(methodOptionsBuilder.build())
          .withKalixOptions(buildJWTOptions(method))
      }

    val valueEntitySubscriptionMethods = component.getMethods
      .filterNot(hasHandleDeletes)
      .filter(hasValueEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>

        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
        addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, transform = true)

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(methodOptionsBuilder.build())
          .withKalixOptions(buildJWTOptions(method))
      }

    (handleDeletesMethods ++ valueEntitySubscriptionMethods).toSeq
  }

  private def subscriptionForTypeLevelValueEntity(
      component: Class[_],
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String) = {
    // create a virtual method
    val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

    val entityType = findValueEntityType(component)
    methodOptionsBuilder.setEventing(eventingInForValueEntity(entityType, handleDeletes = false))

    addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, transform = false)
    val kalixOptions = methodOptionsBuilder.build()

    if (findHandleDeletes(component)) {
      val deleteMethodOptionsBuilder = kalix.MethodOptions.newBuilder()
      deleteMethodOptionsBuilder.setEventing(eventingInForValueEntity(entityType, handleDeletes = true))
      addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, deleteMethodOptionsBuilder, transform = false)
      Seq(
        KalixMethod(VirtualServiceMethod(component, "OnChange", tableType)).withKalixOptions(kalixOptions),
        KalixMethod(VirtualDeleteServiceMethod(component, "OnDelete")).withKalixOptions(
          deleteMethodOptionsBuilder.build()))
    } else {
      Seq(KalixMethod(VirtualServiceMethod(component, "OnChange", tableType)).withKalixOptions(kalixOptions))
    }
  }

  private def addTableOptionsToUpdateMethod(
      tableName: String,
      tableProtoMessage: String,
      builder: MethodOptions.Builder,
      transform: Boolean) = {
    val update = kalix.View.Update
      .newBuilder()
      .setTable(tableName)
      .setTransformUpdates(transform)

    val jsonSchema = kalix.JsonSchema
      .newBuilder()
      .setOutput(tableProtoMessage)
      .build()

    val view = kalix.View
      .newBuilder()
      .setUpdate(update)
      .setJsonSchema(jsonSchema)
      .build()
    builder.setView(view)
  }

}
