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

package com.akkaserverless.scalasdk.valueentity

import akka.actor.ActorSystem
import com.akkaserverless.javasdk.ServiceCallFactory

import scala.jdk.CollectionConverters._
import com.akkaserverless.javasdk.valueentity.{ ValueEntityProvider => Impl }
import com.akkaserverless.javasdk.valueentity.{ ValueEntity => EntityImpl }
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.ServiceCall
import com.akkaserverless.scalasdk.Context
import com.akkaserverless.javasdk.impl.valueentity.{ ValueEntityEffectImpl => JValueEntityEffectImpl }
import com.akkaserverless.javasdk.{ SideEffect => JSideEffect }
import com.akkaserverless.javasdk.impl.effect.{ SideEffectImpl => JSideEffectImpl }

private final class CommandContextImpl(
    val entityId: String,
    val commandName: String,
    val commandId: Long,
    val metadata: Metadata,
    system: ActorSystem,
    serviceCallFactory: ServiceCallFactory)
    extends CommandContext
    // FIXME with AbstractContext
    // FIXME with ActivatableContext {
    {
  def toJava =
    new com.akkaserverless.javasdk.impl.valueentity.CommandContextImpl(
      entityId,
      commandName,
      commandId,
      metadata.impl,
      serviceCallFactory,
      system)
}

private final class ValueEntityContextImpl(
    entityId: String,
    system: ActorSystem,
    serviceCallFactory: ServiceCallFactory) {
  def toJava =
    new com.akkaserverless.javasdk.impl.valueentity.ValueEntityContextImpl(entityId, serviceCallFactory, system)
}
//FIXME
//extends ValueEntityContext
//with AbstractContext

// FIXME implement
trait CommandContext {

  /**
   * The name of the command being executed.
   *
   * @return
   *   The name of the command.
   */
  def commandName: String

  /**
   * The id of the command being executed.
   *
   * @return
   *   The id of the command.
   */
  def commandId: Long

  /** Get the metadata associated with this context. */
  def metadata: Metadata

  /**
   * The id of the entity that this context is for.
   *
   * @return
   *   The entity id.
   */
  def entityId: String
}

//FIXME implement (the impl and type projection is temporary!)
/** @param [S] The type of the state for this entity. */
abstract class ValueEntity[S](val impl: EntityImpl[S]) {
  type Impl = EntityImpl[S]
  private var _commandContext: Option[CommandContext] = None

  /**
   * Implement by returning the initial empty state object. This object will be passed into the command handlers, until
   * a new state replaces it.
   *
   * <p>Also known as "zero state" or "neutral state".
   *
   * <p><code>null</code> is an allowed value.
   */
  def emptyState: S

  /**
   * Additional context and metadata for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected def commandContext(): CommandContext = {
    try {
      _commandContext.get
    } catch {
      case _: NoSuchElementException =>
        throw new IllegalStateException("CommandContext is only available when handling a command.")
    }
  }

  /** INTERNAL API */
  final def _internalSetCommandContext(context: Option[CommandContext]): Unit = {
    _commandContext = context;
  }

  protected def effects: Effect.Builder[S] = ValueEntityEffectImpl[S]()

}

object ValueEntityEffectImpl {
  def apply[S](): ValueEntityEffectImpl[S] = ValueEntityEffectImpl(new JValueEntityEffectImpl())
  def apply[S](impl: JValueEntityEffectImpl[S]): ValueEntityEffectImpl[S] = ValueEntityEffectImpl(impl)
}

class ValueEntityEffectImpl[S](impl: JValueEntityEffectImpl[S])
    extends Effect.Builder[S]
    with Effect.OnSuccessBuilder[S]
    with Effect[S] {
  def deleteState: Effect.OnSuccessBuilder[S] = ValueEntityEffectImpl(impl.deleteState())
  def error[T](description: String): Effect[T] = ValueEntityEffectImpl(impl.error[T](description))
  def forward[T](serviceCall: com.akkaserverless.scalasdk.ServiceCall): Effect[T] = ValueEntityEffectImpl(
    impl.forward(serviceCall.impl))
  def noReply[T]: Effect[T] = ValueEntityEffectImpl(impl.noReply[T]())
  def reply[T](message: T, metadata: com.akkaserverless.scalasdk.Metadata): Effect[T] = ValueEntityEffectImpl(
    impl.reply(message, metadata.impl))
  def reply[T](message: T): Effect[T] = ValueEntityEffectImpl(impl.reply(message))
  def updateState(newState: S): Effect.OnSuccessBuilder[S] = ValueEntityEffectImpl(impl.updateState(newState))
  def addSideEffects(sideEffects: Seq[SideEffect]): Effect[S] = ValueEntityEffectImpl(
    impl.addSideEffects(sideEffects.map(_.impl.asInstanceOf[JSideEffect]).asJavaCollection))
  def thenForward[T](serviceCall: com.akkaserverless.scalasdk.ServiceCall): Effect[T] = ValueEntityEffectImpl(
    impl.thenForward(serviceCall.impl))
  def thenNoReply[T]: Effect[T] = ValueEntityEffectImpl(impl.thenNoReply())
  def thenReply[T](message: T, metadata: com.akkaserverless.scalasdk.Metadata): Effect[T] = ValueEntityEffectImpl(
    impl.thenReply(message, metadata.impl))
  def thenReply[T](message: T): Effect[T] = ValueEntityEffectImpl(impl.thenReply(message))
}

/** A side effect. */
trait SideEffect {

  /** The service call that is executed as this effect. */
  def serviceCall: ServiceCall

  /** Whether this effect should be executed synchronously or not. */
  def synchronous: Boolean
  // FIXME
  def impl: JSideEffectImpl
}

final case class SideEffectImpl(serviceCall: ServiceCall, synchronous: Boolean) extends SideEffect {
  def impl: JSideEffectImpl = ???
  // probably new JSideEffectImpl { ... } , fill in blanks.
}

object SideEffect {

  /**
   * Create an effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @param synchronous
   *   Whether this effect should be executed synchronously.
   * @return
   *   The effect.
   */
  def of(serviceCall: ServiceCall, synchronous: Boolean): SideEffect = {
    new SideEffectImpl(serviceCall, synchronous)
  }

  /**
   * Create an effect of the given service call.
   *
   * @param serviceCall
   *   The service call to effect.
   * @return
   *   The effect.
   */
  def of(serviceCall: ServiceCall): SideEffect = {
    return new SideEffectImpl(serviceCall, false)
  }
}

object Effect {

  /**
   * Construct the effect that is returned by the command handler. The effect describes next processing actions, such as
   * updating state and sending a reply.
   *
   * @param [S]
   *   The type of the state for this entity.
   */
  trait Builder[S] {

    def updateState(newState: S): OnSuccessBuilder[S]

    def deleteState: OnSuccessBuilder[S]

    /**
     * Create a message reply.
     *
     * @param message
     *   The payload of the reply.
     * @return
     *   A message reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def reply[T](message: T): Effect[T]

    /**
     * Create a message reply.
     *
     * @param message
     *   The payload of the reply.
     * @param metadata
     *   The metadata for the message.
     * @return
     *   A message reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def reply[T](message: T, metadata: Metadata): Effect[T]

    /**
     * Create a forward reply.
     *
     * @param serviceCall
     *   The service call representing the forward.
     * @return
     *   A forward reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def forward[T](serviceCall: ServiceCall): Effect[T]

    /**
     * Create an error reply.
     *
     * @param description
     *   The description of the error.
     * @return
     *   An error reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def error[T](description: String): Effect[T]

    /**
     * Create a reply that contains neither a message nor a forward nor an error.
     *
     * <p>This may be useful for emitting effects without sending a message.
     *
     * @return
     *   The reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def noReply[T]: Effect[T]
  }

  trait OnSuccessBuilder[S] {

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param message
     *   The payload of the reply.
     * @return
     *   A message reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def thenReply[T](message: T): Effect[T]

    /**
     * Reply after for example <code>updateState</code>.
     *
     * @param message
     *   The payload of the reply.
     * @param metadata
     *   The metadata for the message.
     * @return
     *   A message reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def thenReply[T](message: T, metadata: Metadata): Effect[T]

    /**
     * Create a forward reply after for example <code>updateState</code>.
     *
     * @param serviceCall
     *   The service call representing the forward.
     * @return
     *   A forward reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def thenForward[T](serviceCall: ServiceCall): Effect[T]

    /**
     * Create a reply that contains neither a message nor a forward nor an error.
     *
     * <p>This may be useful for emitting effects without sending a message.
     *
     * @return
     *   The reply.
     * @param [T]
     *   The type of the message that must be returned by this call.
     */
    def thenNoReply[T]: Effect[T]
  }
}

/**
 * A return type to allow returning forwards or failures, and attaching effects to messages.
 *
 * @param [T]
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
