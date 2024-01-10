/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl

import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.Parser
import com.google.protobuf.{ Message => JavaMessage }

/**
 * A resolved service method.
 */
final case class ResolvedServiceMethod[I, O](
    descriptor: Descriptors.MethodDescriptor,
    inputType: ResolvedType[I],
    outputType: ResolvedType[O]) {

  def outputStreamed: Boolean = descriptor.isServerStreaming
  def name: String = descriptor.getName

  def method(): Descriptors.MethodDescriptor = descriptor
}

/**
 * A resolved type
 */
trait ResolvedType[T] {

  /**
   * Parse the given bytes into this type.
   */
  def parseFrom(bytes: ByteString): T

}

private final class JavaPbResolvedType[T <: JavaMessage](parser: Parser[T]) extends ResolvedType[T] {
  override def parseFrom(bytes: ByteString): T = parser.parseFrom(bytes)
}

private final class ScalaPbResolvedType[T <: scalapb.GeneratedMessage](companion: scalapb.GeneratedMessageCompanion[_])
    extends ResolvedType[T] {
  override def parseFrom(bytes: ByteString): T = companion.parseFrom(bytes.newCodedInput()).asInstanceOf[T]
}

trait ResolvedEntityFactory {
  // TODO JavaDoc
  def resolvedMethods: Map[String, ResolvedServiceMethod[_, _]]
}
