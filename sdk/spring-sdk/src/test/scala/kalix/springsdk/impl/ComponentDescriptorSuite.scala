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

import com.google.api.{ AnnotationsProto, HttpRule }

import scala.reflect.ClassTag
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.MethodOptions
import org.scalatest.matchers.should.Matchers

trait ComponentDescriptorSuite extends Matchers {

  def assertDescriptor[E](assertFunc: ComponentDescriptor => Unit)(implicit ev: ClassTag[E]) = {
    val descriptor = ComponentDescriptor.descriptorFor[E]
    withClue(descriptor.fileDescriptor.toProto.toString) {
      assertFunc(descriptor)
    }
  }

  def assertRequestFieldJavaType(method: ComponentMethod, fieldName: String, expectedType: JavaType) = {
    val field = findField(method, fieldName)
    field.getJavaType shouldBe expectedType
  }

  def assertRequestFieldMessageType(method: ComponentMethod, fieldName: String, expectedMessageType: String) = {
    val field = findField(method, fieldName)
    field.getMessageType.getFullName shouldBe expectedMessageType
  }

  def assertEntityKeyField(method: ComponentMethod, fieldName: String) = {
    val field = findField(method, fieldName)
    val fieldOption = field.toProto.getOptions.getExtension(kalix.Annotations.field)
    fieldOption.getEntityKey shouldBe true
  }

  private def findMethod(desc: ComponentDescriptor, methodName: String) = {
    val grpcMethod = desc.serviceDescriptor.findMethodByName(methodName)
    if (grpcMethod != null) grpcMethod
    else throw new NoSuchElementException(s"Method '$methodName' not found")
  }

  def findKalixMethodOptions(desc: ComponentDescriptor, methodName: String): MethodOptions =
    findMethod(desc, methodName).toProto.getOptions.getExtension(kalix.Annotations.method)

  def findHttpRule(desc: ComponentDescriptor, methodName: String): HttpRule =
    findMethod(desc, methodName).toProto.getOptions.getExtension(AnnotationsProto.http)

  private def findField(method: ComponentMethod, fieldName: String): Descriptors.FieldDescriptor = {
    val field = method.requestMessageDescriptor.findFieldByName(fieldName)
    if (field == null) throw new NoSuchElementException(s"no field found for $fieldName")
    field
  }
}
