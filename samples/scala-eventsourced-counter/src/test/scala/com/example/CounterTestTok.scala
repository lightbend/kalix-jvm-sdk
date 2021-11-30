package com.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
import com.example
import com.google.protobuf.empty.Empty
import scala.collection.immutable.Seq
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.impl.InternalContext
import com.akkaserverless.scalasdk.eventsourcedentity.CommandContext


// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
object CounterTestTok {
  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  def apply(entityFactory: EventSourcedEntityContext => Counter): CounterTestTok =
    apply("testkit-entity-id", entityFactory)
  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  def apply(entityId: String, entityFactory: EventSourcedEntityContext => Counter): CounterTestTok =
    new CounterTestTok(entityFactory(new TestKitEventSourcedEntityContext(entityId)))
}
final class CounterTestTok private(entity: Counter) {
  private var _state: CounterState = entity.emptyState
  private var events: Seq[Any] = Nil

  /** @return The current state of the entity */
  def currentState: CounterState = _state

  /** @return All events emitted by command handlers of this entity up to now */
  def allEvents: Seq[Any] = events

  private def handleEvent(state: CounterState, event: Any): CounterState =
   event match {
     case e: ValueIncreased =>
      entity.valueIncreased(state, e)

    case e: ValueDecreased =>
      entity.valueDecreased(state, e)

    case e: ValueReset =>
      entity.valueReset(state, e)
   }
///HOW do I really call the the effects? 
    // see 63 through the entity
  private def interpretEffects[R](effect: EventSourcedEntity.Effect[R]): EventSourcedResult[R] = {
    //When I execute From EventSourcedEntitiesRouter.handleCommand or handleEvent
    // There is context in the signature's input Q
    //Q how the sequence in the context is used?
    entity._internalSetCommandContext(Some(new StubCommandContextImpl()))
    val events = EventSourcedResultImpl.eventsOf(effect)
    this.events ++= events
    this._state = events.foldLeft(this._state)(handleEvent)
    new EventSourcedResultImpl[R, CounterState](effect, _state)
  }

  def increase(command: example.IncreaseValue): EventSourcedResult[Empty] =
    interpretEffects(entity.increase(_state, command))

  def increaseWithSideEffect(command: example.IncreaseValue): EventSourcedResult[Empty] =
    interpretEffects(entity.increaseWithSideEffect(_state, command))

  def decrease(command: example.DecreaseValue): EventSourcedResult[Empty] =
    interpretEffects(entity.decrease(_state, command))

  def reset(command: example.ResetValue): EventSourcedResult[Empty] =
    interpretEffects(entity.reset(_state, command))

  def getCurrentCounter(command: example.GetCounter): EventSourcedResult[example.CurrentCounter] =
    interpretEffects(entity.getCurrentCounter(_state, command))

    

}

class StubCommandContextImpl(
      override val entityId: String = "whocares",
      override val sequenceNumber: Long = 1L,
      override val commandName: String = "whocares",
      override val commandId: Long = 1L,
      override val metadata: Metadata = Metadata.empty)
      extends CommandContext with InternalContext {
        override def materializer(): akka.stream.Materializer  = ???
        final def getComponentGrpcClient[T](serviceClass: Class[T]): T = ???
      }
