/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.reflection

import scala.jdk.OptionConverters._

import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.Metadata
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.ErrorHandling.BadRequestException

/**
 * Extracts method parameters from an invocation context for the purpose of passing them to a reflective invocation call
 */
trait ParameterExtractor[-C, +T] {
  def extract(context: C): T
}

trait MetadataContext {
  def metadata: Metadata
}

trait DynamicMessageContext {
  def message: DynamicMessage
}

object ParameterExtractors {

  private def toAny(dm: DynamicMessage) = {
    val bytes = dm.getField(JavaPbAny.getDescriptor.findFieldByName("value")).asInstanceOf[ByteString]
    val typeUrl = dm.getField(JavaPbAny.getDescriptor.findFieldByName("type_url")).asInstanceOf[String]
    // TODO: avoid creating a new JavaPbAny instance
    // we want to reuse the typeUrl validation and reading logic (skip tag + jackson reader) from JsonSupport
    // we need a new internal version that also handle DynamicMessages
    JavaPbAny
      .newBuilder()
      .setTypeUrl(typeUrl)
      .setValue(bytes)
      .build()
  }
  private def decodeParam[T](dm: DynamicMessage, cls: Class[T]): T = {
    if (cls == classOf[Array[Byte]]) {
      val bytes = dm.getField(JavaPbAny.getDescriptor.findFieldByName("value")).asInstanceOf[ByteString]
      AnySupport.decodePrimitiveBytes(bytes).toByteArray.asInstanceOf[T]
    } else {
      JsonSupport.decodeJson(cls, toAny(dm))
    }
  }

  private def decodeParamCollection[T, C <: java.util.Collection[T]](
      dm: DynamicMessage,
      cls: Class[T],
      collectionType: Class[C]): C =
    JsonSupport.decodeJsonCollection(cls, collectionType, toAny(dm))

  case class AnyBodyExtractor[T](cls: Class[_]) extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T =
      decodeParam(context.message, cls.asInstanceOf[Class[T]])
  }

  class BodyExtractor[T](field: Descriptors.FieldDescriptor, cls: Class[_])
      extends ParameterExtractor[DynamicMessageContext, T] {

    override def extract(context: DynamicMessageContext): T = {
      context.message.getField(field) match {
        case dm: DynamicMessage => decodeParam(dm, cls.asInstanceOf[Class[T]])
      }
    }
  }

  class CollectionBodyExtractor[T, C <: java.util.Collection[T]](
      field: Descriptors.FieldDescriptor,
      cls: Class[T],
      collectionType: Class[C])
      extends ParameterExtractor[DynamicMessageContext, C] {

    override def extract(context: DynamicMessageContext): C = {
      context.message.getField(field) match {
        case dm: DynamicMessage => decodeParamCollection(dm, cls, collectionType)
      }
    }
  }

  class FieldExtractor[T](field: Descriptors.FieldDescriptor, required: Boolean, deserialize: AnyRef => T)
      extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T = {
      (required, field.isRepeated || context.message.hasField(field)) match {
        case (_, true) => deserialize(context.message.getField(field))
        //we know that currently this applies only to request parameters
        case (true, false)  => throw BadRequestException(s"Required request parameter is missing: ${field.getName}")
        case (false, false) => null.asInstanceOf[T] //could be mapped to optional later on
      }
    }
  }

  class HeaderExtractor[T >: Null](name: String, deserialize: String => T)
      extends ParameterExtractor[MetadataContext, T] {
    override def extract(context: MetadataContext): T = context.metadata.get(name).toScala.map(deserialize).orNull
  }
}
