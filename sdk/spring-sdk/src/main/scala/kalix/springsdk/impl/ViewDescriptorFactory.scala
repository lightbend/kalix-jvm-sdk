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

import kalix.Eventing
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import kalix.MethodOptions
import kalix.springsdk.annotations.Query
import kalix.springsdk.annotations.Subscribe
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.springsdk.impl.ComponentDescriptorFactory.combineByES
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.findValueEntityType
import kalix.springsdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.ReflectionUtils
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.springsdk.impl.ComponentDescriptorFactory.hasUpdateEffectOutput
import kalix.springsdk.impl.reflection.ServiceIntrospectionException
import kalix.springsdk.impl.reflection.SubscriptionServiceMethod
import kalix.springsdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.springsdk.impl.reflection.VirtualServiceMethod
import reactor.core.publisher.Flux

private[impl] object ViewDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    // View class type parameter declares table type
    val tableType: Class[_] =
      component.getGenericSuperclass.asInstanceOf[ParameterizedType].getActualTypeArguments.head.asInstanceOf[Class[_]]

    val tableName: String = component.getAnnotation(classOf[Table]).value()
    if (tableName == null || tableName.trim.isEmpty) {
      throw InvalidComponentException(s"Table name for [${component.getName}] is empty, must be a non-empty string.")
    }
    val tableTypeDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(tableType)
    val tableProtoMessageName = tableTypeDescriptor.mainMessageDescriptor.getName

    val hasMethodLevelEventSourcedEntitySubs = component.getMethods.exists(hasEventSourcedEntitySubscription)
    val hasTypeLevelEventSourcedEntitySubs = hasEventSourcedEntitySubscription(component)
    val hasTypeLevelValueEntitySubs = component.getAnnotation(classOf[Subscribe.ValueEntity]) != null
    val hasMethodLevelValueEntitySubs = component.getMethods.exists(hasValueEntitySubscription)

    val updateMethods = {
      if (hasTypeLevelValueEntitySubs && hasMethodLevelValueEntitySubs)
        throw InvalidComponentException(
          "Mixed usage of @Subscribe.ValueEntity annotations. " +
          "You should either use it at type level or at method level, not both.")

      if (hasTypeLevelValueEntitySubs)
        subscriptionForTypeLevelValueEntity(component, tableType, tableName, tableProtoMessageName)
      else if (hasMethodLevelValueEntitySubs)
        subscriptionForMethodLevelValueEntity(component, tableType, tableName, tableProtoMessageName)
      else if (hasMethodLevelEventSourcedEntitySubs || hasTypeLevelEventSourcedEntitySubs) {
        if (hasMethodLevelEventSourcedEntitySubs && hasTypeLevelEventSourcedEntitySubs)
          throw InvalidComponentException(
            s"You cannot use Subscribe annotation in both the methods and the class. You can do either one or the other.")

        val kalixSubscriptionMethods =
          eventSourcedEntitySubscription(component, tableType, tableName, tableProtoMessageName)
        combineByES(kalixSubscriptionMethods, messageCodec, component)
      } else
        Seq.empty

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

      if (annotatedQueryMethods.isEmpty)
        throw ServiceIntrospectionException(
          component,
          s"No valid query method found. " +
          "Views should have a method annotated with @Query and exposed by a REST annotation")

      if (annotatedQueryMethods.size > 1)
        throw ServiceIntrospectionException(
          component,
          "Views can have only one method annotated with @Query, " +
          s"found ${annotatedQueryMethods.size}.")

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
        if (queryOutputType == tableType) tableTypeDescriptor
        else ProtoMessageDescriptors.generateMessageDescriptors(queryOutputType)

      val queryInputSchemaDescriptor =
        queryMethod.params.find(_.isInstanceOf[BodyParameter]).map { case BodyParameter(param, _) =>
          ProtoMessageDescriptors.generateMessageDescriptors(param.getParameterType)
        }

      val queryAnnotation = queryMethod.javaMethod.getAnnotation(classOf[Query])
      val queryStr = queryAnnotation.value()

      if (queryAnnotation.streamUpdates() && !queryMethod.streamOut)
        throw ServiceIntrospectionException(
          queryMethod.javaMethod,
          "Query.streamUpdates can only be enabled in stream methods returning Flux")

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
    val additionalMessages = Set(tableTypeDescriptor, queryOutputSchemaDescriptor) ++ queryInputSchemaDescriptor.toSet
    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = AclDescriptorFactory.serviceLevelAclAnnotation(component),
      component.getPackageName,
      kalixMethods,
      additionalMessages.toSeq)
  }

  private def eventSourcedEntitySubscription(
      component: Class[_],
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String): Seq[KalixMethod] = {

    def getMethodsWithSubscription(component: Class[_]): Seq[Method] = {
      val methodsTypeLevel: Array[Method] =
        if (hasEventSourcedEntitySubscription(component))
          component.getMethods
            .filter(hasUpdateEffectOutput)
        else Array.empty
      val methodsMethodLevel = component.getMethods
        .filter(hasEventSourcedEntitySubscription)

      if (!methodsTypeLevel.isEmpty && !methodsMethodLevel.isEmpty) {
        throw InvalidComponentException("Can't annotate the class and individual methods at the same time.")
      }
      (methodsTypeLevel ++ validateSameType(methodsMethodLevel, tableType)).toSeq // this means one or the other
    }

    def validateSameType(methods: Seq[Method], tableType: Class[_]): Seq[Method] = {

      def invalidComponentException(method: Method, extraMessage: String) = {
        throw InvalidComponentException(
          s"Method [${method.getName}] annotated with '@Subscribe.EventSourcedEntity' should either receive " +
          "a single parameter of one of the event types or " +
          s"two ordered parameters  of type [${tableType.getName}] and an event type. $extraMessage")
      }

      import ReflectionUtils.methodOrdering
      var previousEntityClass: Option[Class[_]] = None

      methods.sorted // make sure we get the methods in deterministic order
        .map { method =>
          // validate that all updates return the same type
          val entityClass = method.getAnnotation(classOf[Subscribe.EventSourcedEntity]).value().asInstanceOf[Class[_]]

          previousEntityClass match {
            case Some(`entityClass`) => // ok
            case Some(other) =>
              throw InvalidComponentException(
                s"All update methods must return the same type, but [${method.getName}] returns [${entityClass.getName}] while a previous update method returns [${other.getName}]")
            case None => previousEntityClass = Some(entityClass)
          }

          method.getParameterTypes.toList match {
            case params if params.size > 2 =>
              invalidComponentException(
                method,
                s"Subscription method should not have more than 2 parameters, found ${params.size}")
            case _ => // happy days, dev did good with the signature
          }
        }
      methods
    }

    def getEventing(method: Method, component: Class[_]): Eventing =
      if (hasEventSourcedEntitySubscription(component)) eventingInForEventSourcedEntity(component)
      else eventingInForEventSourcedEntity(method)

    getMethodsWithSubscription(component).map { method =>
      // event sourced or topic subscription updates
      val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
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
      tableProtoMessageName: String) = {
    var previousEntityClass: Option[Class[_]] = None

    import ReflectionUtils.methodOrdering

    def invalidComponentException(method: Method, stateClass: Class[_], extraMessage: String) = {
      throw InvalidComponentException(
        s"Method [${method.getName}] annotated with '@Subscribe.ValueEntity' should either receive " +
        s"a single parameter of type [${stateClass.getName}] or " +
        s"two ordered parameters of type [${tableType.getName}, ${stateClass.getName}]. $extraMessage")
    }

    component.getMethods
      .filter(hasValueEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        // validate that all updates return the same type
        val entityClass = method.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]
        val stateClass = entityClass.getGenericSuperclass
          .asInstanceOf[ParameterizedType]
          .getActualTypeArguments
          .head
          .asInstanceOf[Class[_]]
        previousEntityClass match {
          case Some(`entityClass`) => // ok
          case Some(other) =>
            throw InvalidComponentException(
              s"All update methods must return the same type, but [${method.getName}] returns [${entityClass.getName}] while a previous update method returns [${other.getName}]")
          case None => previousEntityClass = Some(entityClass)
        }

        method.getParameterTypes.toList match {
          case params if params.size > 2 =>
            invalidComponentException(
              method,
              stateClass,
              s"Subscription method should have only one parameter, found ${params.size}")
          case _ => // happy days, dev did good with the signature
        }

        // event sourced or topic subscription updates
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
        addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

        KalixMethod(SubscriptionServiceMethod(method))
          .withKalixOptions(methodOptionsBuilder.build())
          .withKalixOptions(buildJWTOptions(method))
      }
      .toSeq
  }

  private def subscriptionForTypeLevelValueEntity(
      component: Class[_],
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String) = {
    // create a virtual method
    val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

    // validate
    val valueEntityClass: Class[_] =
      component.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]
    val entityStateClass = valueEntityStateClassOf(valueEntityClass)
    if (entityStateClass != tableType)
      throw InvalidComponentException(
        s"View subscribes to ValueEntity [${valueEntityClass.getName}] and subscribes to state changes " +
        s"which will be of type [${entityStateClass.getName}] but view type parameter is [${tableType.getName}] which does not match, " +
        "the types of the entity and the subscribing must be the same.")

    val entityType = findValueEntityType(component)
    methodOptionsBuilder.setEventing(eventingInForValueEntity(entityType))

    addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, false)
    val kalixOptions = methodOptionsBuilder.build()

    Seq(
      KalixMethod(VirtualServiceMethod(component, "OnChange", tableType))
        .withKalixOptions(kalixOptions))
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

  private def valueEntityStateClassOf(valueEntityClass: Class[_]): Class[_] = {
    valueEntityClass.getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments
      .head
      .asInstanceOf[Class[_]]
  }
}
