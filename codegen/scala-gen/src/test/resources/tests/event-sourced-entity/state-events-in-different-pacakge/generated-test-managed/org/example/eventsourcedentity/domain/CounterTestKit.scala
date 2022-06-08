package org.example.eventsourcedentity.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.scalasdk.testkit.EventSourcedResult
import kalix.scalasdk.testkit.MockRegistry
import kalix.scalasdk.testkit.impl.EventSourcedEntityEffectsRunner
import kalix.scalasdk.testkit.impl.EventSourcedResultImpl
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityCommandContext
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityEventContext
import org.example.eventsourcedentity
import org.example.eventsourcedentity.state.CounterState

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
object CounterTestKit {
  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   * @param mockRegistry  A registry to be provided in cases which the entity calls other components to allow for unit testing.
   */
  def apply(entityFactory: EventSourcedEntityContext => Counter, mockRegistry: MockRegistry = MockRegistry.empty): CounterTestKit =
    apply("testkit-entity-id", entityFactory, mockRegistry)

  /**
   * Create a testkit instance of Counter with a specific entity id.
   * @param entityId      An entity identifier
   * @param entityFactory A function that creates a Counter based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   * @param mockRegistry  A registry to be provided in cases which the entity calls other components to allow for unit testing.
   */
  def apply(entityId: String, entityFactory: EventSourcedEntityContext => Counter, mockRegistry: MockRegistry): CounterTestKit =
    new CounterTestKit(entityFactory(new TestKitEventSourcedEntityContext(entityId, mockRegistry)))
}
final class CounterTestKit private(entity: Counter) extends EventSourcedEntityEffectsRunner[CounterState](entity: Counter) {

  override protected def handleEvent(state: CounterState, event: Any): CounterState = {
    event match {
      case e: org.example.eventsourcedentity.events.Increased =>
        entity.increased(state, e)

      case e: org.example.eventsourcedentity.events.Decreased =>
        entity.decreased(state, e)
    }
  }

  def increase(command: eventsourcedentity.IncreaseValue, metadata: Metadata = Metadata.empty): EventSourcedResult[Empty] =
    interpretEffects(() => entity.increase(currentState, command), metadata)

  def decrease(command: eventsourcedentity.DecreaseValue, metadata: Metadata = Metadata.empty): EventSourcedResult[Empty] =
    interpretEffects(() => entity.decrease(currentState, command), metadata)
}
