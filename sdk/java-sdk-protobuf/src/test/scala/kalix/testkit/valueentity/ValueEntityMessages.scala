/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.valueentity

import kalix.protocol.component._
import kalix.protocol.entity.Command
import kalix.protocol.value_entity._
import kalix.testkit.entity.EntityMessages
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Message => JavaPbMessage }
import io.grpc.Status
import scalapb.{ GeneratedMessage => ScalaPbMessage }

object ValueEntityMessages extends EntityMessages {
  import ValueEntityAction.Action._
  import ValueEntityStreamIn.{ Message => InMessage }
  import ValueEntityStreamOut.{ Message => OutMessage }

  case class Effects(sideEffects: Seq[SideEffect] = Seq.empty, valueEntityAction: Option[ValueEntityAction] = None) {

    def withUpdateAction(message: JavaPbMessage): Effects =
      copy(valueEntityAction = Some(ValueEntityAction(Update(ValueEntityUpdate(messagePayload(message))))))

    def withUpdateAction(message: ScalaPbMessage): Effects =
      copy(valueEntityAction = Some(ValueEntityAction(Update(ValueEntityUpdate(messagePayload(message))))))

    def withDeleteAction(): Effects =
      copy(valueEntityAction = Some(
        ValueEntityAction(Delete(ValueEntityDelete(cleanupAfter =
          Some(com.google.protobuf.duration.Duration.of(7 * 24 * 60 * 60, 0 /* 7 days default config */ )))))))

    def withSideEffect(service: String, command: String, message: ScalaPbMessage, synchronous: Boolean): Effects =
      withSideEffect(service, command, messagePayload(message), synchronous)

    private def withSideEffect(
        service: String,
        command: String,
        payload: Option[ScalaPbAny],
        synchronous: Boolean): Effects =
      copy(sideEffects = sideEffects :+ SideEffect(service, command, payload, synchronous))
  }

  object Effects {
    val empty: Effects = Effects()
  }

  val EmptyInMessage: InMessage = InMessage.Empty

  def init(serviceName: String, entityId: String): InMessage =
    init(serviceName, entityId, Some(ValueEntityInitState()))

  def init(serviceName: String, entityId: String, payload: JavaPbMessage): InMessage =
    init(serviceName, entityId, ValueEntityInitState(messagePayload(payload)))

  def init(serviceName: String, entityId: String, payload: ScalaPbMessage): InMessage =
    init(serviceName, entityId, ValueEntityInitState(messagePayload(payload)))

  def init(serviceName: String, entityId: String, state: ValueEntityInitState): InMessage =
    init(serviceName, entityId, Some(state))

  def init(serviceName: String, entityId: String, state: Option[ValueEntityInitState]): InMessage =
    InMessage.Init(ValueEntityInit(serviceName, entityId, state))

  def state(payload: JavaPbMessage): ValueEntityInitState =
    ValueEntityInitState(messagePayload(payload))

  def state(payload: ScalaPbMessage): ValueEntityInitState =
    ValueEntityInitState(messagePayload(payload))

  def command(id: Long, entityId: String, name: String): InMessage =
    command(id, entityId, name, EmptyJavaMessage)

  def command(id: Long, entityId: String, name: String, payload: JavaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: ScalaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: Option[ScalaPbAny]): InMessage =
    InMessage.Command(Command(entityId, id, name, payload))

  def reply(id: Long, payload: JavaPbMessage): OutMessage =
    reply(id, messagePayload(payload), None)

  def reply(id: Long, payload: JavaPbMessage, effects: Effects): OutMessage =
    reply(id, messagePayload(payload), effects)

  def reply(id: Long, payload: ScalaPbMessage): OutMessage =
    reply(id, messagePayload(payload), None)

  def reply(id: Long, payload: ScalaPbMessage, effects: Effects): OutMessage =
    reply(id, messagePayload(payload), effects)

  private def reply(id: Long, payload: Option[ScalaPbAny], crudAction: Option[ValueEntityAction]): OutMessage =
    OutMessage.Reply(ValueEntityReply(id, clientActionReply(payload), Seq.empty, crudAction))

  private def reply(id: Long, payload: Option[ScalaPbAny], effects: Effects): OutMessage =
    OutMessage.Reply(ValueEntityReply(id, clientActionReply(payload), effects.sideEffects, effects.valueEntityAction))

  def forward(id: Long, service: String, command: String, payload: ScalaPbMessage): OutMessage =
    forward(id, service, command, payload, Effects.empty)

  def forward(id: Long, service: String, command: String, payload: ScalaPbMessage, effects: Effects): OutMessage =
    forward(id, service, command, messagePayload(payload), effects)

  private def forward(
      id: Long,
      service: String,
      command: String,
      payload: Option[ScalaPbAny],
      effects: Effects): OutMessage =
    replyAction(id, clientActionForward(service, command, payload), effects)

  private def replyAction(id: Long, action: Option[ClientAction], effects: Effects): OutMessage =
    OutMessage.Reply(ValueEntityReply(id, action, effects.sideEffects, effects.valueEntityAction))

  def actionFailure(id: Long, description: String): OutMessage =
    OutMessage.Reply(ValueEntityReply(id, clientActionFailure(id, description)))

  def actionFailure(id: Long, description: String, statusCode: Status.Code): OutMessage =
    OutMessage.Reply(ValueEntityReply(id, clientActionFailure(id, description, statusCode.value())))

  def failure(description: String): OutMessage =
    failure(id = 0, description)

  def failure(id: Long, description: String): OutMessage =
    OutMessage.Failure(Failure(id, description))

  def update(state: JavaPbMessage): Effects =
    Effects.empty.withUpdateAction(state)

  def update(state: ScalaPbMessage): Effects =
    Effects.empty.withUpdateAction(state)

  def delete(): Effects =
    Effects.empty.withDeleteAction()
}
