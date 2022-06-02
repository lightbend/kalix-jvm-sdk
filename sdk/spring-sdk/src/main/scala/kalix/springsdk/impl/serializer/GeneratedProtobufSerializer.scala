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

package kalix.springsdk.impl.serializer

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.MapHasAsJava

import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.UnsafeByteOperations
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.impl.ByteStringEncoding
import kalix.serializer.Serializer
import kalix.springsdk.impl.serializer.GeneratedProtobufSerializer.KalixGeneratedProto

object GeneratedProtobufSerializer {

  final val KalixGeneratedProto = "proto.kalix.io/"

  private val mapper = new ProtobufMapper()

  def buildSerializers(classLoader: ClassLoader, descriptor: FileDescriptor): Map[Class[_], Serializer] = {

    descriptor.getMessageTypes.asScala.map { messageType =>
      val packageName = descriptor.getFile.getPackage
      val className = packageName + "." + messageType.getName
      val classType = classLoader.loadClass(className)

      val serializer = new GeneratedProtobufSerializer(mapper, classType)
      (classType, serializer)
    }.toMap

  }

  def buildSerializersJava(classLoader: ClassLoader, descriptor: FileDescriptor): java.util.Map[Class[_], Serializer] =
    buildSerializers(classLoader, descriptor).asJava
}

class GeneratedProtobufSerializer(mapper: ProtobufMapper, classType: Class[_]) extends Serializer {

  private val schemaWrapper = mapper.generateSchemaFor(classType)

  override def serialize(any: Any): JavaPbAny = {
    val bytes =
      UnsafeByteOperations.unsafeWrap(
        mapper
          .writer(schemaWrapper)
          .writeValueAsBytes(any))

    JavaPbAny
      .newBuilder()
      .setTypeUrl(s"${KalixGeneratedProto}${classType.getName}")
      .setValue(bytes)
      .build()
  }

  override def deserialize(bytes: ByteString): Any =
    mapper
      .readerFor(classType)
      .`with`(schemaWrapper)
      .readValue(bytes.toByteArray)
}
