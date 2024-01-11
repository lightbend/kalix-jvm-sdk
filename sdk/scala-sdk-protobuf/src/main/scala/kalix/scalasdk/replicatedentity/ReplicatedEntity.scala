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

package kalix.scalasdk.replicatedentity

import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.impl.replicatedentity.ReplicatedEntityEffectImpl
import io.grpc.Status

object ReplicatedEntity {
  object Effect {

    /**
     * Construct the effect that is returned by the command handler. The effect describes next processing actions, such
     * as sending a reply or deleting an entity.
     *
     * @tparam D
     *   The replicated data type for this entity.
     */
    trait Builder[D] {

      /** Update the underlying replicated data for the replicated entity. */
      def update(newData: D): Effect.OnSuccessBuilder

      /**
       * Delete the replicated entity.
       *
       * When a replicated entity is deleted, it may not be created again. Additionally, replicated entity deletion
       * results in tombstones that get accumulated for the life of the cluster. If you expect to delete replicated
       * entities frequently, it's recommended that you store them in a single or sharded [[ReplicatedMap]], rather than
       * individual replicated entities.
       */
      def delete: Effect.OnSuccessBuilder

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def reply[T](message: T): ReplicatedEntity.Effect[T]

      /**
       * Create a message reply.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def reply[T](message: T, metadata: Metadata): ReplicatedEntity.Effect[T]

      /**
       * Create a forward reply.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def forward[T](serviceCall: DeferredCall[_, T]): ReplicatedEntity.Effect[T]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @return
       *   An error reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def error[T](description: String): ReplicatedEntity.Effect[T]

      /**
       * Create an error reply.
       *
       * @param description
       *   The description of the error.
       * @param statusCode
       *   A gRPC status code.
       * @return
       *   An error reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def error[T](description: String, statusCode: Status.Code): ReplicatedEntity.Effect[T]
    }

    trait OnSuccessBuilder {

      /**
       * Reply after for example <code>delete</code>.
       *
       * @param message
       *   The payload of the reply.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](message: T): ReplicatedEntity.Effect[T]

      /**
       * Reply after for example <code>delete</code>.
       *
       * @param message
       *   The payload of the reply.
       * @param metadata
       *   The metadata for the message.
       * @return
       *   A message reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenReply[T](message: T, metadata: Metadata): ReplicatedEntity.Effect[T]

      /**
       * Create a forward reply after for example <code>delete</code>.
       *
       * @param serviceCall
       *   The service call representing the forward.
       * @return
       *   A forward reply.
       * @tparam T
       *   The type of the message that must be returned by this call.
       */
      def thenForward[T](serviceCall: DeferredCall[_, T]): ReplicatedEntity.Effect[T]
    }
  }

  /**
   * An Effect is a description of what Kalix needs to do after the command is handled. You can think of it as a set of
   * instructions you are passing to Kalix. Kalix will process the instructions on your behalf and ensure that any data
   * that needs to be persisted will be persisted.
   *
   * Each Kalix component defines its own effects, which are a set of predefined operations that match the capabilities
   * of that component.
   *
   * A Replicated Effect can either:
   *
   *   - update the entity state and send a reply to the caller
   *   - directly reply to the caller if the command is not requesting any state change
   *   - rejected the command by returning an error
   *   - instruct Kalix to delete the entity
   *
   * @tparam T
   *   The type of the message that must be returned by this call.
   */
  trait Effect[T] {

    /**
     * Attach the given side effects to this reply.
     *
     * @param sideEffects
     *   The effects to attach.
     * @return
     *   A new reply with the attached effects.
     */
    def addSideEffects(sideEffects: Seq[SideEffect]): Effect[T]

  }
}

abstract class ReplicatedEntity[D <: ReplicatedData] {
  private var _commandContext: Option[CommandContext] = None

  /**
   * Implement by returning the initial empty replicated data object. This object will be passed into the command
   * handlers.
   *
   * Also known as the "zero" or "neutral" state.
   *
   * The initial data cannot be `null`.
   *
   * @param factory
   *   the factory to create the initial empty replicated data object
   */
  def emptyData(factory: ReplicatedDataFactory): D

  /**
   * Additional context and metadata for a command handler.
   *
   * It will throw an exception if accessed from constructor.
   */
  protected final def commandContext(): CommandContext =
    _commandContext.getOrElse(
      throw new IllegalStateException("CommandContext is only available when handling a command."))

  /** INTERNAL API */
  final def _internalSetCommandContext(context: Option[CommandContext]): Unit =
    _commandContext = context

  protected final def effects: ReplicatedEntity.Effect.Builder[D] =
    ReplicatedEntityEffectImpl()
}
