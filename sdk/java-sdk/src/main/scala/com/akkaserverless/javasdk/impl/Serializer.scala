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

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.JsonSupport
// FIXME: should we duplicate this in the scala sdk and expose ScalaPbAny instead?
import com.google.protobuf.{ Any => JavaPbAny }

object Serializer {

  def noopSerializer: Serializer = NoopSerializer

  object NoopSerializer extends Serializer {
    override def canSerialize(any: Any): Boolean = false

    override def serialize(any: Any): JavaPbAny =
      throw new IllegalArgumentException("NoopSerializer can not serialize types")

    override def canDeserialize(any: JavaPbAny): Boolean = false

    override def deserialize(any: JavaPbAny): Any =
      throw new IllegalArgumentException("NoopSerializer can not deserialize types")

  }

}
object Serializers {
  def add(serializer: Serializer): CompositeSerializer =
    new CompositeSerializer().add(serializer)
}

trait Serializer {
  def canSerialize(any: Any): Boolean
  def serialize(any: Any): JavaPbAny

  def canDeserialize(any: JavaPbAny): Boolean
  def deserialize(any: JavaPbAny): Any
}

final class JsonSerializer(cls: Class[_]) extends Serializer {
  override def canSerialize(any: Any): Boolean =
    cls == any.getClass

  override def serialize(any: Any): JavaPbAny =
    JsonSupport.encodeJson(any)

  override def canDeserialize(any: JavaPbAny): Boolean =
    any.getTypeUrl.contains(cls.getName)

  override def deserialize(any: JavaPbAny): Any =
    JsonSupport.decodeJson(cls, any)

}

object CompositeSerializer {
  def add(serializer: Serializer): CompositeSerializer = new CompositeSerializer(Seq(serializer))
}

class CompositeSerializer(serializers: Seq[Serializer]) extends Serializer {

  def this() = this(Seq.empty[Serializer])

  def add(serializer: Serializer): CompositeSerializer =
    new CompositeSerializer(serializers :+ serializer)

  override def canSerialize(any: Any): Boolean = serializers.exists(_.canSerialize(any))

  override def serialize(any: Any): JavaPbAny =
    serializers
      .collectFirst {
        case s if s.canSerialize(any) => s.serialize(any)
      }
      .getOrElse(throw new IllegalArgumentException(s"No serializer found for type ${any.getClass.getName}"))

  override def canDeserialize(any: JavaPbAny): Boolean = serializers.exists(_.canDeserialize(any))

  override def deserialize(any: JavaPbAny): Any =
    serializers
      .collectFirst {
        case d if d.canDeserialize(any) => d.deserialize(any)
      }
      .getOrElse(throw new IllegalArgumentException(s"No deserializer found for type ${any.getClass.getName}"))
}
