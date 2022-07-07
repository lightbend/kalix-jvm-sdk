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

import kalix.springsdk.annotations.{ Entity, Query, Subscribe, Table }
import kalix.springsdk.impl.ComponentDescriptorFactory.{
  eventingInForValueEntity,
  findValueEntityType,
  hasValueEntitySubscription,
  validateRestMethod
}
import kalix.springsdk.impl.ViewDescriptorFactory.valueEntityStateClassOf
import kalix.springsdk.impl.reflection._
import kalix.{ EventSource, Eventing, MethodOptions }

import java.lang.reflect.{ Method, Modifier, ParameterizedType }

object ComponentDescriptorFactory {

  def hasValueEntitySubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.ValueEntity]) != null

  def findValueEntityType(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.ValueEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[Entity]).entityType()
  }

  def findValueEntityType(component: Class[_]): String = {
    val ann = component.getAnnotation(classOf[Subscribe.ValueEntity])
    val entityClass = ann.value()
    entityClass.getAnnotation(classOf[Entity]).entityType()
  }

  def eventingInForValueEntity(javaMethod: Method): Eventing = {
    val entityType = findValueEntityType(javaMethod)
    val eventSource = EventSource.newBuilder().setValueEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForValueEntity(entityType: String): Eventing = {
    val eventSource = EventSource.newBuilder().setValueEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def validateRestMethod(javaMethod: Method): Boolean =
    if (hasValueEntitySubscription(javaMethod))
      throw new IllegalArgumentException(
        "Methods annotated with Kalix @Subscription annotations" +
        " can not be annotated with REST annotations ")
    else true

}

sealed trait ComponentDescriptorFactory[T] {
  def component: Class[T]

  def buildDescriptor(nameGenerator: NameGenerator): ComponentDescriptor
}

case class ActionDescriptorFactory[T](component: Class[T]) extends ComponentDescriptorFactory[T] {

  private def kalixMethods: Seq[KalixMethod] = {
    val springAnnotatedMethods =
      RestServiceIntrospector.inspectService(component).methods.map { serviceMethod =>
        validateRestMethod(serviceMethod.javaMethod)
        KalixMethod(serviceMethod)
      }

    val subscriptionMethods = component.getMethods
      .filter(hasValueEntitySubscription)
      .map { method =>
        val subscriptionOptions = eventingInForValueEntity(method)
        val kalixOptions =
          kalix.MethodOptions.newBuilder().setEventing(subscriptionOptions).build()

        KalixMethod(RestServiceMethod(method))
          .withKalixOptions(kalixOptions)
      }

    springAnnotatedMethods ++ subscriptionMethods
  }

  override def buildDescriptor(nameGenerator: NameGenerator): ComponentDescriptor = {
    val serviceName = nameGenerator.getName(component.getSimpleName)
    kalixMethods.foldLeft(new ComponentDescriptor(serviceName, component.getPackageName, nameGenerator)) {
      (desc, method) =>
        desc.withMethod(method)
    }
  }
}

case class EntityDescriptorFactory[T](component: Class[T]) extends ComponentDescriptorFactory[T] {

  private val entityKeys: Seq[String] = component.getAnnotation(classOf[Entity]).entityKey()

  private def kalixMethods: Seq[KalixMethod] =
    RestServiceIntrospector.inspectService(component).methods.map { restMethod =>
      KalixMethod(restMethod, entityKeys = entityKeys)
    }

  override def buildDescriptor(nameGenerator: NameGenerator): ComponentDescriptor = {
    val serviceName = nameGenerator.getName(component.getSimpleName)
    kalixMethods.foldLeft(new ComponentDescriptor(serviceName, component.getPackageName, nameGenerator)) {
      (desc, method) =>
        desc.withMethod(method)
    }
  }
}

object ViewDescriptorFactory {
  private def valueEntityStateClassOf(valueEntityClass: Class[_]): Class[_] = {
    valueEntityClass.getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments
      .head
      .asInstanceOf[Class[_]]
  }
}

case class ViewDescriptorFactory[T](component: Class[T]) extends ComponentDescriptorFactory[T] {

  private val hasTypeLevelValueEntitySubs = component.getAnnotation(classOf[Subscribe.ValueEntity]) != null
  private val hasMethodLevelValueEntitySubs = component.getMethods.exists(hasValueEntitySubscription)
  // View class type parameter declares table type
  private val tableType: Class[_] =
    component.getGenericSuperclass.asInstanceOf[ParameterizedType].getActualTypeArguments.head.asInstanceOf[Class[_]]
  private val tableTypeDescriptor = MessageDescriptor.generateMessageDescriptors(tableType)

  private val updateMethods = {
    if (hasTypeLevelValueEntitySubs && hasMethodLevelValueEntitySubs)
      throw new IllegalArgumentException(
        "Mixed usage of @Subscribe.ValueEntity annotations. " +
        "You should either use it at type level or at method level, not both.")

    if (hasTypeLevelValueEntitySubs) {
      // create a virtual method
      val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

      // validate
      val valueEntityClass: Class[_] =
        component.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]
      val entityStateClass = valueEntityStateClassOf(valueEntityClass)
      if (entityStateClass != tableType)
        throw new IllegalArgumentException(
          s"View subscribes to ValueEntity [${valueEntityClass.getName}] and subscribes to state changes " +
          s"which will be of type [${entityStateClass.getName}] but view type parameter is [${tableType.getName}] which does not match, " +
          "the types of the entity and the subscribing must be the same.")

      val entityType = findValueEntityType(component)
      methodOptionsBuilder.setEventing(eventingInForValueEntity(entityType))

      addTableOptionsToUpdateMethod(methodOptionsBuilder, false)
      val kalixOptions = methodOptionsBuilder.build()

      Seq(
        KalixMethod(VirtualServiceMethod(component, "OnChange"))
          .withKalixOptions(kalixOptions))

    } else {
      var previousValueEntityClass: Option[Class[_]] = None

      component.getMethods
        .filter(hasValueEntitySubscription)
        .map { method =>
          // validate
          val valueEntityClass = method.getAnnotation(classOf[Subscribe.ValueEntity]).value().asInstanceOf[Class[_]]
          previousValueEntityClass match {
            case Some(`valueEntityClass`) => // ok
            case Some(other) =>
              throw new IllegalArgumentException(
                s"All update methods must return the same type, but [${method.getName}] returns [${valueEntityClass.getName}] while a prevous update method returns [${other.getName}]")
            case None => previousValueEntityClass = Some(valueEntityClass)
          }
          // FIXME validate that transform method accepts value entity state type

          // event sourced or topic subscription updates
          val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
          methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
          addTableOptionsToUpdateMethod(methodOptionsBuilder, true)

          KalixMethod(RestServiceMethod(method)).withKalixOptions(methodOptionsBuilder.build())

        }
        .toSeq
    }
  }

  // we only take methods with Query annotations and Spring REST annotations
  val (queryMethod, queryResultDescriptor) = {
    val annotatedMethods = RestServiceIntrospector
      .inspectService(component)
      .methods
      .filter(_.javaMethod.getAnnotation(classOf[Query]) != null)
    if (annotatedMethods.isEmpty)
      throw new IllegalArgumentException(
        "No method annotated with @Query found. " +
        "Views should have a method annotated with @Query")
    if (annotatedMethods.size > 1)
      throw new IllegalArgumentException("Views can have only one method annotated with @Query")

    val annotatedMethod = annotatedMethods.head

    val returnType = annotatedMethod.javaMethod.getReturnType
    val returnTypeDescriptor =
      if (returnType == tableType) tableTypeDescriptor
      else MessageDescriptor.generateMessageDescriptors(returnType)

    val queryStr = annotatedMethod.javaMethod.getAnnotation(classOf[Query]).value()

    val query = kalix.View.Query
      .newBuilder()
      .setQuery(queryStr)
      .build()

    val jsonSchema = kalix.JsonSchema
      .newBuilder()
      // FIXME request type
      .setOutput(returnTypeDescriptor.mainMessageDescriptor.getName)
      .build()

    val view = kalix.View
      .newBuilder()
      .setJsonSchema(jsonSchema)
      .setQuery(query)
      .build()

    val builder = kalix.MethodOptions.newBuilder()
    builder.setView(view)
    val methodOptions = builder.build()

    (KalixMethod(annotatedMethod).withKalixOptions(methodOptions), returnTypeDescriptor)
  }

  private val kalixMethods: Seq[KalixMethod] = queryMethod +: updateMethods

  private def addTableOptionsToUpdateMethod(builder: MethodOptions.Builder, transform: Boolean) = {
    val tableName: String = component.getAnnotation(classOf[Table]).value()
    if (tableName == null || tableName.trim.isEmpty) {
      // TODO: find a better error message for this
      throw new IllegalArgumentException(s"Invalid table name for view ${component.getName}.")
    }

    val update = kalix.View.Update
      .newBuilder()
      .setTable(tableName)
      .setTransformUpdates(transform)

    val jsonSchema = kalix.JsonSchema
      .newBuilder()
      .setOutput(tableTypeDescriptor.mainMessageDescriptor.getName)
      .build()

    val view = kalix.View
      .newBuilder()
      .setUpdate(update)
      .setJsonSchema(jsonSchema)
      .build()
    builder.setView(view)
  }

  override def buildDescriptor(nameGenerator: NameGenerator): ComponentDescriptor = {
    val serviceName = nameGenerator.getName(component.getSimpleName)
    val componentDescriptor =
      kalixMethods.foldLeft(new ComponentDescriptor(serviceName, component.getPackageName, nameGenerator)) {
        (desc, method) =>
          desc.withMethod(method)
      }
    componentDescriptor.withMessageDescriptor(tableTypeDescriptor)
  }
}
