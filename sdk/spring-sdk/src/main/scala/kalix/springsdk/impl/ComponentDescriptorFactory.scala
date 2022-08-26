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

import kalix.springsdk.annotations.Table
import kalix.springsdk.annotations.{ Entity, Subscribe }
import kalix.springsdk.impl.reflection._
import kalix.{ EventSource, Eventing }

import java.lang.reflect.{ Method, Modifier }

private[impl] object ComponentDescriptorFactory {

  def hasValueEntitySubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.ValueEntity]) != null

  def hasTopicSubscription(javaMethod: Method): Boolean =
    Modifier.isPublic(javaMethod.getModifiers) &&
    javaMethod.getAnnotation(classOf[Subscribe.Topic]) != null

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

  def findTopicName(javaMethod: Method): String = {
    val ann = javaMethod.getAnnotation(classOf[Subscribe.Topic])
    ann.value()
  }

  def eventingInForValueEntity(javaMethod: Method): Eventing = {
    val entityType = findValueEntityType(javaMethod)
    val eventSource = EventSource.newBuilder().setValueEntity(entityType).build()
    Eventing.newBuilder().setIn(eventSource).build()
  }

  def eventingInForTopic(javaMethod: Method): Eventing = {
    val topicName = findTopicName(javaMethod)
    val eventSource = EventSource.newBuilder().setTopic(topicName).build()
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

  // TODO: add more validations here
  // we should let users know if components are missing required annotations,
  // eg: entities require @Entity, view require @Table and @Subscription
  def getFactoryFor(component: Class[_]): ComponentDescriptorFactory = {
    if (component.getAnnotation(classOf[Entity]) != null)
      EntityDescriptorFactory
    else if (component.getAnnotation(classOf[Table]) != null)
      ViewDescriptorFactory
    else
      ActionDescriptorFactory
  }

}

private[impl] trait ComponentDescriptorFactory {

  /**
   * Inspect the component class (type), validate the annotations/methods and build a component descriptor for it.
   */
  def buildDescriptorFor(componentClass: Class[_], nameGenerator: NameGenerator): ComponentDescriptor
}

/**
 * Thrown when the component has incorrect annotations
 */
final case class InvalidComponentException(message: String) extends RuntimeException(message)
