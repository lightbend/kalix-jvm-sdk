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

import kalix.MethodOptions
import kalix.springsdk.annotations.Query
import kalix.springsdk.annotations.Subscribe
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForValueEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.findValueEntityType
import kalix.springsdk.impl.ComponentDescriptorFactory.hasValueEntitySubscription
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.SpringRestServiceMethod
import kalix.springsdk.impl.reflection.ReflectionUtils
import kalix.springsdk.impl.reflection.RestServiceIntrospector.BodyParameter
import java.lang.reflect.ParameterizedType

import kalix.springsdk.impl.ComponentDescriptorFactory.eventingInForEventSourcedEntity
import kalix.springsdk.impl.ComponentDescriptorFactory.hasEventSourcedEntitySubscription
import kalix.springsdk.impl.reflection.RestServiceMethod
import kalix.springsdk.impl.reflection.VirtualServiceMethod
import reactor.core.publisher.Flux

private[impl] object ViewDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(component: Class[_], nameGenerator: NameGenerator): ComponentDescriptor = {

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
      else if (hasMethodLevelEventSourcedEntitySubs)
        eventSourcedEntitySubscription(component, tableType, tableName, tableProtoMessageName)
      else
        Seq.empty

    }

    // we only take methods with Query annotations and Spring REST annotations
    val (
      queryMethod: KalixMethod,
      queryInputSchemaDescriptor: Option[ProtoMessageDescriptors],
      queryOutputSchemaDescriptor: ProtoMessageDescriptors) = {
      val annotatedMethods = RestServiceIntrospector
        .inspectService(component)
        .methods
        .filter(_.javaMethod.getAnnotation(classOf[Query]) != null)
      if (annotatedMethods.isEmpty)
        throw new IllegalArgumentException(
          s"No valid query method found in class ${component.getName}. " +
          "Views should have a method annotated with @Query and exposed by a REST annotation")
      if (annotatedMethods.size > 1)
        throw new IllegalArgumentException(
          "Views can have only one method annotated with @Query, " +
          s"found ${annotatedMethods.size} in class ${component.getName}")

      val annotatedMethod: SpringRestServiceMethod = annotatedMethods.head

      val queryOutputType = {
        val returnType = annotatedMethod.javaMethod.getReturnType
        if (returnType == classOf[Flux[_]]) {
          annotatedMethod.javaMethod.getGenericReturnType
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
        annotatedMethod.params.find(_.isInstanceOf[BodyParameter]).map { case BodyParameter(param, _) =>
          ProtoMessageDescriptors.generateMessageDescriptors(param.getParameterType)
        }
      val queryStr = annotatedMethod.javaMethod.getAnnotation(classOf[Query]).value()

      val query = kalix.View.Query
        .newBuilder()
        .setQuery(queryStr)
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
        KalixMethod(annotatedMethod.copy(callable = false), methodOptions = Seq(methodOptions)),
        queryInputSchemaDescriptor,
        queryOutputSchemaDescriptor)
    }

    val kalixMethods: Seq[KalixMethod] = queryMethod +: updateMethods
    val serviceName = nameGenerator.getName(component.getSimpleName)
    val additionalMessages = Set(tableTypeDescriptor, queryOutputSchemaDescriptor) ++ queryInputSchemaDescriptor.toSet
    ComponentDescriptor(nameGenerator, serviceName, component.getPackageName, kalixMethods, additionalMessages.toSeq)
  }

  private def eventSourcedEntitySubscription(
      component: Class[_],
      tableType: Class[_],
      tableName: String,
      tableProtoMessageName: String) = {
    var previousEntityClass: Option[Class[_]] = None

    import ReflectionUtils.methodOrdering

    def invalidComponentException(method: Method, extraMessage: String) = {
      throw InvalidComponentException(
        s"Method [${method.getName}] annotated with '@Subscribe.EventSourcedEntity' should either receive " +
        "a single parameter of one of the event types or " +
        s"two ordered parameters  of type [${tableType.getName}] and an event type. $extraMessage")
    }

    component.getMethods
      .filter(hasEventSourcedEntitySubscription)
      .sorted // make sure we get the methods in deterministic order
      .map { method =>
        // validate that all updates return the same type
        val entityClass = method.getAnnotation(classOf[Subscribe.EventSourcedEntity]).value().asInstanceOf[Class[_]]
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
              s"Subscription method should have one or two params, found ${params.size}")
          case p1 :: Nil if p1 == tableType =>
            invalidComponentException(method, "Subscription method only has the view type.")

          case p1 :: p2 :: Nil if p1 != tableType =>
            invalidComponentException(method, s"Subscription method first param must be view type [$tableType]")

          case p1 :: p2 :: Nil if p1 == tableType && p2 == tableType =>
            invalidComponentException(method, s"Subscription method receives twice the view type [$tableType]")
          case _ => // happy days, dev did good with the signature
        }

        // event sourced or topic subscription updates
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForEventSourcedEntity(method))
        addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

        KalixMethod(RestServiceMethod(method, method.getParameterCount - 1))
          .withKalixOptions(methodOptionsBuilder.build())

      }
      .toSeq
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
              s"Subscription method should have one or two params, found ${params.size}")
          case p1 :: Nil if p1 != stateClass =>
            invalidComponentException(method, stateClass, s"Single parameter is not of type $stateClass")
          case p1 :: p2 :: Nil if p1 != tableType || p2 != stateClass =>
            invalidComponentException(
              method,
              stateClass,
              s"Parameters of type [${tableType.getName}, ${stateClass.getName}]")
          case _ => // happy days, dev did good with the signature
        }

        // event sourced or topic subscription updates
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
        methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
        addTableOptionsToUpdateMethod(tableName, tableProtoMessageName, methodOptionsBuilder, true)

        KalixMethod(RestServiceMethod(method, method.getParameterCount - 1))
          .withKalixOptions(methodOptionsBuilder.build())

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
