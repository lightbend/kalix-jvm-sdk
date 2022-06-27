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
  hasSubscription,
  validateRestMethod
}
import kalix.springsdk.impl.ViewDescriptorFactory.{ noQueryAnnotationMessage, onlyOneQueryAnnotationMessage }
import kalix.springsdk.impl.reflection._
import kalix.{ EventSource, Eventing, MethodOptions }

import java.lang.reflect.{ Method, Modifier }

object ComponentDescriptorFactory {

  def hasSubscription(javaMethod: Method): Boolean =
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
    if (hasSubscription(javaMethod))
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
      .filter(hasSubscription)
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
  val noQueryAnnotationMessage = "No method annotated with @Query found. " +
    "Views should have a method annotated with @Query"
  val onlyOneQueryAnnotationMessage = "Views can have only one method annotated with @Query"
}
case class ViewDescriptorFactory[T](component: Class[T]) extends ComponentDescriptorFactory[T] {

  private def addTableOptions(builder: MethodOptions.Builder, transform: Boolean) = {
    val tableName: String = component.getAnnotation(classOf[Table]).value()
    if (tableName == null || tableName.trim.isEmpty) {
      // TODO: find a better error message for this
      throw new IllegalArgumentException(s"Invalid table name for view ${component.getName}.")
    }

    val update = kalix.View.Update
      .newBuilder()
      .setTable(tableName)
      .setTransformUpdates(transform)

    val view = kalix.View.newBuilder().setUpdate(update).build()
    builder.setView(view)
  }

  private def queryOptions(queryStr: String) = {
    val builder = kalix.MethodOptions.newBuilder()
    val query = kalix.View.Query.newBuilder().setQuery(queryStr).build()
    val view = kalix.View.newBuilder().setQuery(query).build()
    builder.setView(view)
    builder.build()
  }

  private val hasTypeLevelValueEntitySubs = component.getAnnotation(classOf[Subscribe.ValueEntity]) != null

  private def kalixMethods: Seq[KalixMethod] = {

    def validQueryMethod(javaMethod: Method) =
      validateRestMethod(javaMethod) && javaMethod.getAnnotation(classOf[Query]) != null

    // we only take methods with Query annotations and Spring REST annotations
    val springAnnotatedMethods =
      RestServiceIntrospector.inspectService(component).methods.collect {
        case serviceMethod if validQueryMethod(serviceMethod.javaMethod) =>
          val queryStr = serviceMethod.javaMethod.getAnnotation(classOf[Query]).value()
          KalixMethod(serviceMethod)
            .withKalixOptions(queryOptions(queryStr))
      }

    if (springAnnotatedMethods.isEmpty)
      throw new IllegalArgumentException(noQueryAnnotationMessage)

    if (springAnnotatedMethods.size > 1)
      throw new IllegalArgumentException(onlyOneQueryAnnotationMessage)

    val hasMethodLevelValueEntitySubs = component.getMethods.exists(hasSubscription)

    if (hasTypeLevelValueEntitySubs && hasMethodLevelValueEntitySubs)
      throw new IllegalArgumentException(
        "Mixed usage of @Subscribe.ValueEntity annotations. " +
        "You should either use it at type level or at method level, not both.")

    val subscriptionMethods =
      if (hasTypeLevelValueEntitySubs) {
        // create a virtual method
        val methodOptionsBuilder = kalix.MethodOptions.newBuilder()

        val entityType = findValueEntityType(component)
        methodOptionsBuilder.setEventing(eventingInForValueEntity(entityType))

        addTableOptions(methodOptionsBuilder, false)
        val kalixOptions = methodOptionsBuilder.build()

        Seq(
          KalixMethod(VirtualServiceMethod(component, "OnChange"))
            .withKalixOptions(kalixOptions))

      } else {
        component.getMethods
          .filter(hasSubscription)
          .map { method =>

            val methodOptionsBuilder = kalix.MethodOptions.newBuilder()
            methodOptionsBuilder.setEventing(eventingInForValueEntity(method))
            addTableOptions(methodOptionsBuilder, true)

            KalixMethod(RestServiceMethod(method)).withKalixOptions(methodOptionsBuilder.build())
          }
          .toSeq
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
