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

import com.google.api.AnnotationsProto
import com.google.api.HttpRule
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.MethodOptions
import kalix.ServiceOptions
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.ComponentDescriptor
import kalix.javasdk.impl.ProtoDescriptorRenderer
import kalix.javasdk.impl.JsonMessageCodec
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import scalapb.descriptors.MethodDescriptor

trait ComponentDescriptorSuite extends Matchers {

  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    ComponentDescriptor.descriptorFor(ev.runtimeClass, new JsonMessageCodec)

  def assertDescriptor[E](assertFunc: ComponentDescriptor => Unit)(implicit ev: ClassTag[E]): Unit = {
    val descriptor = descriptorFor[E]
    withClue(ProtoDescriptorRenderer.toString(descriptor.fileDescriptor)) {
      assertFunc(descriptor)
    }
  }

  def assertRequestFieldJavaType(method: CommandHandler, fieldName: String, expectedType: JavaType): Assertion = {
    val field = findField(method, fieldName)
    field.getJavaType shouldBe expectedType
  }

  def assertRequestFieldRequested(method: CommandHandler, fieldName: String, isRequired: Boolean): Assertion = {
    val field = findField(method, fieldName)
    field.isRequired shouldBe isRequired
  }

  def assertRequestFieldNumberAndJavaType(
      method: CommandHandler,
      fieldName: String,
      number: Int,
      expectedType: JavaType): Assertion = {
    val field = findField(method, fieldName)
    field.getJavaType shouldBe expectedType
    field.getNumber shouldBe number
  }

  def assertRequestFieldMessageType(
      method: CommandHandler,
      fieldName: String,
      expectedMessageType: String): Assertion = {
    val field = findField(method, fieldName)
    field.getMessageType.getFullName shouldBe expectedMessageType
  }

  def assertEntityKeyField(method: CommandHandler, fieldName: String): Assertion = {
    val field = findField(method, fieldName)
    val fieldOption = field.toProto.getOptions.getExtension(kalix.Annotations.field)
    fieldOption.getEntityKey shouldBe true
  }

  def findMethodByName(desc: ComponentDescriptor, methodName: String): Descriptors.MethodDescriptor = {
    val grpcMethod = desc.serviceDescriptor.findMethodByName(methodName)
    if (grpcMethod != null) grpcMethod
    else throw new NoSuchElementException(s"Method '$methodName' not found")
  }

  def findKalixMethodOptions(desc: ComponentDescriptor, methodName: String): MethodOptions =
    findKalixMethodOptions(findMethodByName(desc, methodName))

  def findKalixMethodOptions(methodDescriptor: Descriptors.MethodDescriptor): MethodOptions =
    methodDescriptor.toProto.getOptions.getExtension(kalix.Annotations.method)

  def findKalixServiceOptions(desc: ComponentDescriptor): ServiceOptions =
    desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)

  def findHttpRule(desc: ComponentDescriptor, methodName: String): HttpRule =
    findMethodByName(desc, methodName).toProto.getOptions.getExtension(AnnotationsProto.http)

  private def findField(method: CommandHandler, fieldName: String): Descriptors.FieldDescriptor = {
    val field = method.requestMessageDescriptor.findFieldByName(fieldName)
    if (field == null) throw new NoSuchElementException(s"no field found for $fieldName")
    field
  }

//  def findAclExtension
}
