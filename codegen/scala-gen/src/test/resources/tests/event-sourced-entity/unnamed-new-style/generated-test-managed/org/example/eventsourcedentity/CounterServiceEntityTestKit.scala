package org.example.eventsourcedentity

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.scalasdk.testkit.EventSourcedResult
import kalix.scalasdk.testkit.impl.EventSourcedEntityEffectsRunner
import kalix.scalasdk.testkit.impl.EventSourcedResultImpl
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityCommandContext
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
import kalix.scalasdk.testkit.impl.TestKitEventSourcedEntityEventContext
import org.example.eventsourcedentity
import org.example.eventsourcedentity.domain.CounterState

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing CounterServiceEntity
 */
object CounterServiceEntityTestKit {
  /**
   * Create a testkit instance of CounterServiceEntity
   * @param entityFactory A function that creates a CounterServiceEntity based on the given EventSourcedEntityContext,
   *                      a default entity id is used.
   */
  def apply(entityFactory: EventSourcedEntityContext => CounterServiceEntity): CounterServiceEntityTestKit =
    apply("testkit-entity-id", entityFactory)
  /**
   * Create a testkit instance of CounterServiceEntity with a specific entity id.
   */
  def apply(entityId: String, entityFactory: EventSourcedEntityContext => CounterServiceEntity): CounterServiceEntityTestKit =
    new CounterServiceEntityTestKit(entityFactory(new TestKitEventSourcedEntityContext(entityId)))
}
final class CounterServiceEntityTestKit private(entity: CounterServiceEntity) extends EventSourcedEntityEffectsRunner[CounterState](entity: CounterServiceEntity) {

  override protected def handleEvent(state: CounterState, event: Any): CounterState = {
    event match {
      case e: org.example.eventsourcedentity.domain.Increased =>
        entity.increased(state, e)

      case e: org.example.eventsourcedentity.domain.Decreased =>
        entity.decreased(state, e)
    }
  }

  def increase(command: IncreaseValue, metadata: Metadata = Metadata.empty): EventSourcedResult[Empty] =
    interpretEffects(() => entity.increase(currentState, command), metadata)

  def decrease(command: DecreaseValue, metadata: Metadata = Metadata.empty): EventSourcedResult[Empty] =
    interpretEffects(() => entity.decrease(currentState, command), metadata)
}
