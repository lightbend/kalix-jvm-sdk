/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.entity

import kalix.protocol.component._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.empty.{ Empty => ScalaPbEmpty }
import com.google.protobuf.{ Any => JavaPbAny, Empty => JavaPbEmpty, Message => JavaPbMessage, StringValue }
import scalapb.{ GeneratedMessage => ScalaPbMessage }

object EntityMessages extends EntityMessages

trait EntityMessages {
  val EmptyJavaMessage: JavaPbMessage = JavaPbEmpty.getDefaultInstance
  val EmptyScalaMessage: ScalaPbMessage = ScalaPbEmpty.defaultInstance

  def clientActionReply(payload: Option[ScalaPbAny]): Option[ClientAction] =
    Some(ClientAction(ClientAction.Action.Reply(Reply(payload))))

  def clientActionForward(service: String, command: String, payload: Option[ScalaPbAny]): Option[ClientAction] =
    Some(ClientAction(ClientAction.Action.Forward(Forward(service, command, payload))))

  def clientActionFailure(description: String): Option[ClientAction] =
    clientActionFailure(id = 0, description, statusCode = 0)

  def clientActionFailure(id: Long, description: String): Option[ClientAction] =
    clientActionFailure(id, description, statusCode = 0)

  def clientActionFailure(id: Long, description: String, statusCode: Int): Option[ClientAction] =
    Some(ClientAction(ClientAction.Action.Failure(Failure(id, description, statusCode))))

  def sideEffect(service: String, command: String, payload: JavaPbMessage): SideEffect =
    sideEffect(service, command, messagePayload(payload), synchronous = false)

  def sideEffect(service: String, command: String, payload: JavaPbMessage, synchronous: Boolean): SideEffect =
    sideEffect(service, command, messagePayload(payload), synchronous)

  def sideEffect(service: String, command: String, payload: ScalaPbMessage): SideEffect =
    sideEffect(service, command, messagePayload(payload), synchronous = false)

  def sideEffect(service: String, command: String, payload: ScalaPbMessage, synchronous: Boolean): SideEffect =
    sideEffect(service, command, messagePayload(payload), synchronous)

  def sideEffect(service: String, command: String, payload: Option[ScalaPbAny], synchronous: Boolean): SideEffect =
    SideEffect(service, command, payload, synchronous)

  def streamCancelled(entityId: String): StreamCancelled =
    streamCancelled(id = 0, entityId)

  def streamCancelled(id: Long, entityId: String): StreamCancelled =
    StreamCancelled(entityId, id)

  def messagePayload(message: JavaPbMessage): Option[ScalaPbAny] =
    Option(message).map(protobufAny)

  def messagePayload(message: ScalaPbMessage): Option[ScalaPbAny] =
    Option(message).map(protobufAny)

  def protobufAny(message: JavaPbMessage): ScalaPbAny = message match {
    case javaPbAny: JavaPbAny => ScalaPbAny.fromJavaProto(javaPbAny)
    case _ => ScalaPbAny("type.googleapis.com/" + message.getDescriptorForType.getFullName, message.toByteString)
  }

  def protobufAny(message: ScalaPbMessage): ScalaPbAny = message match {
    case scalaPbAny: ScalaPbAny => scalaPbAny
    case _ => ScalaPbAny("type.googleapis.com/" + message.companion.scalaDescriptor.fullName, message.toByteString)
  }

  def primitiveString(value: String): ScalaPbAny =
    ScalaPbAny("type.kalix.io/string", StringValue.of(value).toByteString)

  def readPrimitiveString(any: ScalaPbAny): String =
    if (any.typeUrl == "type.kalix.io/string") {
      val stream = any.value.newCodedInput
      stream.readTag // assume it's for string
      stream.readString
    } else ""
}
