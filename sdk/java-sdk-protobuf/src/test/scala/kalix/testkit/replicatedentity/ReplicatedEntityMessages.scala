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

package kalix.testkit.replicatedentity

import kalix.protocol.replicated_entity._
import kalix.protocol.component.{ ClientAction, Failure, SideEffect }
import kalix.protocol.entity.Command
import kalix.testkit.entity.EntityMessages
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Message => JavaPbMessage }
import io.grpc.Status
import scalapb.{ GeneratedMessage => ScalaPbMessage }

object ReplicatedEntityMessages extends EntityMessages {
  import ReplicatedEntityStreamIn.{ Message => InMessage }
  import ReplicatedEntityStreamOut.{ Message => OutMessage }

  final case class Effects(
      stateAction: Option[ReplicatedEntityStateAction] = None,
      sideEffects: Seq[SideEffect] = Seq.empty) {
    def withSideEffect(service: String, command: String, message: JavaPbMessage): Effects =
      withSideEffect(service, command, messagePayload(message), synchronous = false)

    def withSideEffect(service: String, command: String, message: JavaPbMessage, synchronous: Boolean): Effects =
      withSideEffect(service, command, messagePayload(message), synchronous)

    def withSideEffect(service: String, command: String, message: ScalaPbMessage): Effects =
      withSideEffect(service, command, messagePayload(message), synchronous = false)

    def withSideEffect(service: String, command: String, message: ScalaPbMessage, synchronous: Boolean): Effects =
      withSideEffect(service, command, messagePayload(message), synchronous)

    def withSideEffect(service: String, command: String, payload: Option[ScalaPbAny], synchronous: Boolean): Effects =
      copy(sideEffects = sideEffects :+ SideEffect(service, command, payload, synchronous))

    def ++(other: Effects): Effects =
      Effects(stateAction.orElse(other.stateAction), sideEffects ++ other.sideEffects)
  }

  object Effects {
    val empty: Effects = Effects()
  }

  val EmptyInMessage: InMessage = InMessage.Empty

  def init(serviceName: String, entityId: String): InMessage =
    init(serviceName, entityId, None)

  def init(serviceName: String, entityId: String, delta: ReplicatedEntityDelta.Delta): InMessage =
    InMessage.Init(ReplicatedEntityInit(serviceName, entityId, Option(ReplicatedEntityDelta(delta))))

  def init(serviceName: String, entityId: String, delta: Option[ReplicatedEntityDelta]): InMessage =
    InMessage.Init(ReplicatedEntityInit(serviceName, entityId, delta))

  def delta(delta: ReplicatedEntityDelta.Delta): InMessage =
    InMessage.Delta(ReplicatedEntityDelta(delta))

  val delete: InMessage =
    InMessage.Delete(ReplicatedEntityDelete())

  def command(id: Long, entityId: String, name: String): InMessage =
    command(id, entityId, name, EmptyJavaMessage)

  def command(id: Long, entityId: String, name: String, payload: JavaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: ScalaPbMessage): InMessage =
    command(id, entityId, name, messagePayload(payload))

  def command(id: Long, entityId: String, name: String, payload: Option[ScalaPbAny]): InMessage =
    InMessage.Command(Command(entityId, id, name, payload))

  def reply(id: Long, payload: JavaPbMessage): OutMessage =
    reply(id, payload, Effects.empty)

  def reply(id: Long, payload: JavaPbMessage, effects: Effects): OutMessage =
    reply(id, messagePayload(payload), effects)

  def reply(id: Long, payload: ScalaPbMessage): OutMessage =
    reply(id, payload, Effects.empty)

  def reply(id: Long, payload: ScalaPbMessage, effects: Effects): OutMessage =
    reply(id, messagePayload(payload), effects)

  def reply(id: Long, payload: Option[ScalaPbAny], effects: Effects): OutMessage =
    replicatedEntityReply(id, clientActionReply(payload), effects)

  def forward(id: Long, service: String, command: String, payload: JavaPbMessage): OutMessage =
    forward(id, service, command, payload, Effects.empty)

  def forward(id: Long, service: String, command: String, payload: JavaPbMessage, effects: Effects): OutMessage =
    forward(id, service, command, messagePayload(payload), effects)

  def forward(id: Long, service: String, command: String, payload: ScalaPbMessage): OutMessage =
    forward(id, service, command, payload, Effects.empty)

  def forward(id: Long, service: String, command: String, payload: ScalaPbMessage, effects: Effects): OutMessage =
    forward(id, service, command, messagePayload(payload), effects)

  def forward(id: Long, service: String, command: String, payload: Option[ScalaPbAny], effects: Effects): OutMessage =
    replicatedEntityReply(id, clientActionForward(service, command, payload), effects)

  def failure(description: String): OutMessage =
    failure(id = 0, description)

  def failure(id: Long, description: String): OutMessage =
    failure(id, description, Status.Code.UNKNOWN, Effects.empty)

  def failure(id: Long, description: String, statusCode: Status.Code): OutMessage =
    failure(id, description, statusCode, Effects.empty)

  def failure(id: Long, description: String, statusCode: Status.Code, effects: Effects): OutMessage =
    replicatedEntityReply(id, clientActionFailure(id, description, statusCode.value()), effects)

  def replicatedEntityReply(id: Long, clientAction: Option[ClientAction], effects: Effects): OutMessage =
    OutMessage.Reply(ReplicatedEntityReply(id, clientAction, effects.sideEffects, effects.stateAction))

  def entityFailure(description: String): OutMessage =
    entityFailure(id = 0, description)

  def entityFailure(id: Long, description: String): OutMessage =
    OutMessage.Failure(Failure(id, description))

  def replicatedEntityUpdate(delta: ReplicatedEntityDelta.Delta): Option[ReplicatedEntityStateAction] =
    Some(ReplicatedEntityStateAction(ReplicatedEntityStateAction.Action.Update(ReplicatedEntityDelta(delta))))

  def deltaCounter(value: Long): ReplicatedEntityDelta.Delta.Counter =
    ReplicatedEntityDelta.Delta.Counter(ReplicatedCounterDelta(value))

  final case class DeltaReplicatedSet(
      cleared: Boolean = false,
      removed: Seq[ScalaPbAny] = Seq.empty,
      added: Seq[ScalaPbAny] = Seq.empty) {

    def add(element: JavaPbMessage, elements: JavaPbMessage*): DeltaReplicatedSet =
      add(protobufAny(element), elements.map(protobufAny): _*)

    def add(element: ScalaPbMessage, elements: ScalaPbMessage*): DeltaReplicatedSet =
      add(protobufAny(element), elements.map(protobufAny): _*)

    def add(element: ScalaPbAny, elements: ScalaPbAny*): DeltaReplicatedSet =
      add(element +: elements)

    def add(elements: Seq[ScalaPbAny]): DeltaReplicatedSet =
      copy(added = added ++ elements)

    def remove(element: JavaPbMessage, elements: JavaPbMessage*): DeltaReplicatedSet =
      remove(protobufAny(element), elements.map(protobufAny): _*)

    def remove(element: ScalaPbMessage, elements: ScalaPbMessage*): DeltaReplicatedSet =
      remove(protobufAny(element), elements.map(protobufAny): _*)

    def remove(element: ScalaPbAny, elements: ScalaPbAny*): DeltaReplicatedSet =
      remove(element +: elements)

    def remove(elements: Seq[ScalaPbAny]): DeltaReplicatedSet =
      copy(removed = removed ++ elements)

    def clear(cleared: Boolean = true): DeltaReplicatedSet =
      copy(cleared = cleared)

    def replicatedEntityDelta(): ReplicatedEntityDelta.Delta.ReplicatedSet =
      ReplicatedEntityDelta.Delta.ReplicatedSet(ReplicatedSetDelta(cleared, removed, added))
  }

  object DeltaReplicatedSet {
    val empty: DeltaReplicatedSet = DeltaReplicatedSet()
  }

  def deltaRegister(value: JavaPbMessage): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED)

  def deltaRegister(value: JavaPbMessage, clock: ReplicatedEntityClock): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, clock, customClock = 0L)

  def deltaRegister(
      value: JavaPbMessage,
      clock: ReplicatedEntityClock,
      customClock: Long): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(messagePayload(value), clock, customClock)

  def deltaRegister(value: ScalaPbMessage): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED)

  def deltaRegister(value: ScalaPbMessage, clock: ReplicatedEntityClock): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, clock, customClock = 0L)

  def deltaRegister(
      value: ScalaPbMessage,
      clock: ReplicatedEntityClock,
      customClock: Long): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(messagePayload(value), clock, customClock)

  def deltaRegister(value: Option[ScalaPbAny]): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED)

  def deltaRegister(value: Option[ScalaPbAny], clock: ReplicatedEntityClock): ReplicatedEntityDelta.Delta.Register =
    deltaRegister(value, clock, customClock = 0L)

  def deltaRegister(
      value: Option[ScalaPbAny],
      clock: ReplicatedEntityClock,
      customClock: Long): ReplicatedEntityDelta.Delta.Register =
    ReplicatedEntityDelta.Delta.Register(ReplicatedRegisterDelta(value, clock, customClock))

  final case class DeltaMap(
      cleared: Boolean = false,
      removed: Seq[ScalaPbAny] = Seq.empty,
      updated: Seq[(ScalaPbAny, ReplicatedEntityDelta)] = Seq.empty,
      added: Seq[(ScalaPbAny, ReplicatedEntityDelta)] = Seq.empty) {

    def add(key: JavaPbMessage, delta: ReplicatedEntityDelta): DeltaMap =
      add(protobufAny(key), delta)

    def add(key: ScalaPbMessage, delta: ReplicatedEntityDelta): DeltaMap =
      add(protobufAny(key), delta)

    def add(key: ScalaPbAny, delta: ReplicatedEntityDelta): DeltaMap =
      add(Seq(key -> delta))

    def add(entries: Seq[(ScalaPbAny, ReplicatedEntityDelta)]): DeltaMap =
      copy(added = added ++ entries)

    def update(key: JavaPbMessage, delta: ReplicatedEntityDelta): DeltaMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbMessage, delta: ReplicatedEntityDelta): DeltaMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbAny, delta: ReplicatedEntityDelta): DeltaMap =
      update(Seq(key -> delta))

    def update(entries: Seq[(ScalaPbAny, ReplicatedEntityDelta)]): DeltaMap =
      copy(updated = updated ++ entries)

    def remove(key: JavaPbMessage, keys: JavaPbMessage*): DeltaMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbMessage, keys: ScalaPbMessage*): DeltaMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbAny, keys: ScalaPbAny*): DeltaMap =
      remove(key +: keys)

    def remove(keys: Seq[ScalaPbAny]): DeltaMap =
      copy(removed = removed ++ keys)

    def clear(cleared: Boolean = true): DeltaMap =
      copy(cleared = cleared)

    def replicatedEntityDelta(): ReplicatedEntityDelta.Delta.ReplicatedMap = {
      val updatedEntries = updated.map { case (key, delta) => ReplicatedMapEntryDelta(Option(key), Option(delta)) }
      val addedEntries = added.map { case (key, delta) => ReplicatedMapEntryDelta(Option(key), Option(delta)) }
      ReplicatedEntityDelta.Delta.ReplicatedMap(ReplicatedMapDelta(cleared, removed, updatedEntries, addedEntries))
    }
  }

  object DeltaMap {
    val empty: DeltaMap = DeltaMap()
  }

  final case class DeltaCounterMap(
      cleared: Boolean = false,
      removed: Seq[ScalaPbAny] = Seq.empty,
      updated: Seq[(ScalaPbAny, ReplicatedCounterDelta)] = Seq.empty) {

    def update(key: JavaPbMessage, delta: ReplicatedCounterDelta): DeltaCounterMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbMessage, delta: ReplicatedCounterDelta): DeltaCounterMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbAny, delta: ReplicatedCounterDelta): DeltaCounterMap =
      update(Seq(key -> delta))

    def update(entries: Seq[(ScalaPbAny, ReplicatedCounterDelta)]): DeltaCounterMap =
      copy(updated = updated ++ entries)

    def remove(key: JavaPbMessage, keys: JavaPbMessage*): DeltaCounterMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbMessage, keys: ScalaPbMessage*): DeltaCounterMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbAny, keys: ScalaPbAny*): DeltaCounterMap =
      remove(key +: keys)

    def remove(keys: Seq[ScalaPbAny]): DeltaCounterMap =
      copy(removed = removed ++ keys)

    def clear(cleared: Boolean = true): DeltaCounterMap =
      copy(cleared = cleared)

    def replicatedEntityDelta(): ReplicatedEntityDelta.Delta.ReplicatedCounterMap = {
      val updatedEntries = updated.map { case (key, delta) =>
        ReplicatedCounterMapEntryDelta(Option(key), Option(delta))
      }
      ReplicatedEntityDelta.Delta.ReplicatedCounterMap(ReplicatedCounterMapDelta(cleared, removed, updatedEntries))
    }
  }

  object DeltaCounterMap {
    val empty: DeltaCounterMap = DeltaCounterMap()
  }

  final case class DeltaRegisterMap(
      cleared: Boolean = false,
      removed: Seq[ScalaPbAny] = Seq.empty,
      updated: Seq[(ScalaPbAny, ReplicatedRegisterDelta)] = Seq.empty) {

    def update(key: JavaPbMessage, delta: ReplicatedRegisterDelta): DeltaRegisterMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbMessage, delta: ReplicatedRegisterDelta): DeltaRegisterMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbAny, delta: ReplicatedRegisterDelta): DeltaRegisterMap =
      update(Seq(key -> delta))

    def update(entries: Seq[(ScalaPbAny, ReplicatedRegisterDelta)]): DeltaRegisterMap =
      copy(updated = updated ++ entries)

    def remove(key: JavaPbMessage, keys: JavaPbMessage*): DeltaRegisterMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbMessage, keys: ScalaPbMessage*): DeltaRegisterMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbAny, keys: ScalaPbAny*): DeltaRegisterMap =
      remove(key +: keys)

    def remove(keys: Seq[ScalaPbAny]): DeltaRegisterMap =
      copy(removed = removed ++ keys)

    def clear(cleared: Boolean = true): DeltaRegisterMap =
      copy(cleared = cleared)

    def replicatedEntityDelta(): ReplicatedEntityDelta.Delta.ReplicatedRegisterMap = {
      val updatedEntries = updated.map { case (key, delta) =>
        ReplicatedRegisterMapEntryDelta(Option(key), Option(delta))
      }
      ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(ReplicatedRegisterMapDelta(cleared, removed, updatedEntries))
    }
  }

  object DeltaRegisterMap {
    val empty: DeltaRegisterMap = DeltaRegisterMap()
  }

  final case class DeltaMultiMap(
      cleared: Boolean = false,
      removed: Seq[ScalaPbAny] = Seq.empty,
      updated: Seq[(ScalaPbAny, ReplicatedSetDelta)] = Seq.empty) {

    def update(key: JavaPbMessage, delta: ReplicatedSetDelta): DeltaMultiMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbMessage, delta: ReplicatedSetDelta): DeltaMultiMap =
      update(protobufAny(key), delta)

    def update(key: ScalaPbAny, delta: ReplicatedSetDelta): DeltaMultiMap =
      update(Seq(key -> delta))

    def update(entries: Seq[(ScalaPbAny, ReplicatedSetDelta)]): DeltaMultiMap =
      copy(updated = updated ++ entries)

    def remove(key: JavaPbMessage, keys: JavaPbMessage*): DeltaMultiMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbMessage, keys: ScalaPbMessage*): DeltaMultiMap =
      remove(protobufAny(key), keys.map(protobufAny): _*)

    def remove(key: ScalaPbAny, keys: ScalaPbAny*): DeltaMultiMap =
      remove(key +: keys)

    def remove(keys: Seq[ScalaPbAny]): DeltaMultiMap =
      copy(removed = removed ++ keys)

    def clear(cleared: Boolean = true): DeltaMultiMap =
      copy(cleared = cleared)

    def replicatedEntityDelta(): ReplicatedEntityDelta.Delta.ReplicatedMultiMap = {
      val updatedEntries = updated.map { case (key, delta) => ReplicatedMultiMapEntryDelta(Option(key), Option(delta)) }
      ReplicatedEntityDelta.Delta.ReplicatedMultiMap(ReplicatedMultiMapDelta(cleared, removed, updatedEntries))
    }
  }

  object DeltaMultiMap {
    val empty: DeltaMultiMap = DeltaMultiMap()
  }

  def deltaVote(selfVote: Boolean, votesFor: Int = 0, totalVoters: Int = 0): ReplicatedEntityDelta.Delta.Vote =
    ReplicatedEntityDelta.Delta.Vote(VoteDelta(selfVote, votesFor, totalVoters))

  val replicatedEntityDelete: Option[ReplicatedEntityStateAction] =
    Some(ReplicatedEntityStateAction(ReplicatedEntityStateAction.Action.Delete(ReplicatedEntityDelete())))
}
