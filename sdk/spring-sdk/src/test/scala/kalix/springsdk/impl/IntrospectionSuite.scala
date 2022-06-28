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

import scala.reflect.ClassTag

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.EventSource
import org.scalatest.matchers.should.Matchers

trait IntrospectionSuite extends Matchers {

  def assertDescriptor[E](assertFunc: ComponentDescription => Unit)(implicit ev: ClassTag[E]) = {
    assertFunc(Introspector.inspect(ev.runtimeClass))
  }

  def assertMessage(method: ComponentMethod, fieldName: String, expectedType: JavaType) = {
    val field = findField(method, fieldName)
    field.getJavaType shouldBe expectedType
  }

  def assertEntityKeyField(method: ComponentMethod, fieldName: String) = {
    val field = findField(method, fieldName)
    val fieldOption = field.toProto.getOptions.getExtension(kalix.Annotations.field)
    fieldOption.getEntityKey shouldBe true
  }

  def findSubscription(desc: ComponentDescription, methodName: String): EventSource = {
    val grpcMethod = desc.serviceDescriptor.findMethodByName(methodName)
    val methodOptions = grpcMethod.toProto.getOptions.getExtension(kalix.Annotations.method)
    methodOptions.getEventing.getIn
  }

  private def findField(method: ComponentMethod, fieldName: String): Descriptors.FieldDescriptor = {
    val field = method.messageDescriptor.findFieldByName(fieldName)
    if (field == null) throw new NoSuchElementException(s"no field found for $fieldName")
    field
  }
}
