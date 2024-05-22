/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import scala.reflect.ClassTag
import com.google.api.AnnotationsProto
import com.google.api.HttpRule
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.MethodOptions
import kalix.ServiceOptions
import kalix.javasdk.impl.Validations.Invalid
import kalix.javasdk.impl.Validations.Valid
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait ComponentDescriptorSuite extends Matchers {

  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    ComponentDescriptor.descriptorFor(ev.runtimeClass, new JsonMessageCodec)

  def assertDescriptor[E](assertFunc: ComponentDescriptor => Unit)(implicit ev: ClassTag[E]): Unit = {
    val validation = Validations.validate(ev.runtimeClass)
    validation match { // if any invalid component, log and throw
      case Valid =>
        val descriptor = descriptorFor[E]
        withClue(ProtoDescriptorRenderer.toString(descriptor.fileDescriptor)) {
          assertFunc(descriptor)
        }
      case Invalid(_) => validation.failIfInvalid
    }
  }

  def assertRequestFieldJavaType(method: CommandHandler, fieldName: String, expectedType: JavaType): Assertion = {
    val field = findField(method, fieldName)
    field.getJavaType shouldBe expectedType
  }

  def assertFieldIsProto3Optional(method: CommandHandler, fieldName: String): Assertion = {
    val field: Descriptors.FieldDescriptor = findField(method, fieldName)
    field.isOptional shouldBe true
    val oneofDesc = field.getContainingOneof
    oneofDesc.getName shouldBe "_" + fieldName
    method.requestMessageDescriptor.getOneofs should contain(oneofDesc)
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

  def assertEntityIdField(method: CommandHandler, fieldName: String): Assertion = {
    val field = findField(method, fieldName)
    val fieldOption = field.toProto.getOptions.getExtension(kalix.Annotations.field)
    fieldOption.getId shouldBe true
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
