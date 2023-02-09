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
import java.lang.reflect.ParameterizedType

import kalix.Eventing
import kalix.MethodOptions
import kalix.springsdk.annotations.Query
import kalix.springsdk.annotations.Subscribe
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.springsdk.impl.ComponentDescriptorFactory.combineByES
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntityServiceLevel
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.findEventSourcedEntityType
import kalix.springsdk.impl.ComponentDescriptorFactory.findHandleDeletes
import kalix.springsdk.impl.ComponentDescriptorFactory.findValueEntityType
import kalix.springsdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasHandleDeletes
import kalix.springsdk.impl.ComponentDescriptorFactory.hasUpdateEffectOutput
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.subscribeToEventStream
import kalix.springsdk.impl.reflection.HandleDeletesServiceMethod
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.ReflectionUtils
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.springsdk.impl.reflection.SubscriptionServiceMethod
import kalix.springsdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.springsdk.impl.reflection.VirtualDeleteServiceMethod
import kalix.springsdk.impl.reflection.VirtualServiceMethod
import reactor.core.publisher.Flux

private[impl] object ViewDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    val isMultiTable = KalixServer.isMultiTableView(component)

    val tableComponents =
      if (isMultiTable) component.getDeclaredClasses.toSeq.filter(KalixServer.isNestedViewTable)
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
          val hasTypeLevelValueEntitySubs = component.getAnnotation(classOf[Subscribe.ValueEntity]) != null
          val hasMethodLevelValueEntitySubs = component.getMethods.exists(hasValueEntitySubscription)
          val hasStreamSubscription = component.getAnnotation(classOf[Subscribe.Stream]) != null

          val updateMethods = {
            if (hasTypeLevelValueEntitySubs)
              subscriptionForTypeLevelValueEntity(component, tableType, tableName, tableProtoMessageName)
            else if (hasMethodLevelValueEntitySubs)
              subscriptionForMethodLevelValueEntity(component, tableType, tableName, tableProtoMessageName)
            else if (hasMethodLevelEventSourcedEntitySubs || hasTypeLevelEventSourcedEntitySubs) {

              if (hasTypeLevelEventSourcedEntitySubs) {
                val kalixSubscriptionMethods =
                  methodsForTypeLevelESSubscriptions(component, tableName, tableProtoMessageName, isMultiTable)
                combineByES(kalixSubscriptionMethods, messageCodec, component)

              } else {
                val methodsForMethodLevelESSubscriptions =
                  subscriptionEventSourcedEntityMethodLevel(component, tableType, tableName, tableProtoMessageName)
                combineByES(methodsForMethodLevelESSubscriptions, messageCodec, component)
              }

            } else if (hasStreamSubscription) {
              val kalixSubscriptionMethods =
                methodsForTypeLevelStreamSubscriptions(component, tableName, tableProtoMessageName)
              combineByES(kalixSubscriptionMethods, messageCodec, component)
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

    // we only take methods with Query annotations and Spring REST annotations
    val (
      queryMethod: KalixMethod,
      queryInputSchemaDescriptor: Option[ProtoMessageDescriptors],
      queryOutputSchemaDescriptor: ProtoMessageDescriptors) = {

      val annotatedQueryMethods = RestServiceIntrospector
        .inspectService(component)
        .methods
        .filter(_.javaMethod.getAnnotation(classOf[Query]) != null)

      val queryMethod: SyntheticRequestServiceMethod = annotatedQueryMethods.head

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
      (
        KalixMethod(queryMethod.copy(callable = false), methodOptions = Some(methodOptions))
          .withKalixOptions(buildJWTOptions(queryMethod.javaMethod)),
        queryInputSchemaDescriptor,
        queryOutputSchemaDescriptor)
    }

    val kalixMethods: Seq[KalixMethod] = queryMethod +: updateMethods
    val serviceName = nameGenerator.getName(component.getSimpleName)
    val additionalMessages =
      tableTypeDescriptors.toSet ++ Set(queryOutputSchemaDescriptor) ++ queryInputSchemaDescriptor.toSet

    val serviceLevelOptions = {

      val allOptions =
        AclDescriptorFactory.serviceLevelAclAnnotation(component) ::
        eventingInForEventSourcedEntityServiceLevel(component) ::
        subscribeToEventStream(component) ::
        Nil

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
      kalixMethods,
      additionalMessages.toSeq)
  }

  private def validateSameType(methods: Seq[Method], tableType: Class[_]): Seq[Method] = {

    import ReflectionUtils.methodOrdering
    var previousEntityClass: Option[Class[_]] = None

    methods.sorted // make sure we get the methods in deterministic order
      .map { method =>
        // validate that all updates return the same type
        val entityClass = method.getAnnotation(classOf[Subscribe.EventSourcedEntity]).value().asInstanceOf[Class[_]]

        previousEntityClass match {
          case Some(`entityClass`) => // ok
          case Some(other) =>
            throw InvalidComponentException( // TODO: move this to Validations
              s"All update methods must return the same type, but [${method.getName}] returns [${entityClass.getName}] while a previous update method returns [${other.getName}]")
          case None => previousEntityClass = Some(entityClass)
        }

      }
    methods
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
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {

    def getMethodsWithSubscription(component: Class[_]): Seq[Method] = {
      val methodsMethodLevel =
        component.getMethods
          .filter(hasEventSourcedEntitySubscription)
          .toIndexedSeq
      validateSameType(methodsMethodLevel, tableType) // this means one or the other
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

  private def subscriptionForMethodLevelValueEntity(
      component: Class[_],
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {
    var previousEntityClass: Option[Class[_]] = None

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
        // validate that all updates return the same type
        val entityClass = method.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]

        previousEntityClass match {
          case Some(`entityClass`) => // ok
          case Some(other) =>
            throw InvalidComponentException( // TODO: move this to Validations
              s"All update methods must subscribe to the same type, but [${method.getName}] subscribes to [${entityClass.getName}] while a previous update method subscribes to [${other.getName}]")
          case None => previousEntityClass = Some(entityClass)
        }

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
