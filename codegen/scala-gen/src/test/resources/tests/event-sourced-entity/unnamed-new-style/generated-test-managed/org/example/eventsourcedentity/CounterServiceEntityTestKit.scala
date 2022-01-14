package org.example.eventsourcedentity

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedEntityEffectsRunner
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityCommandContext
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityEventContext
import com.google.protobuf.empty.Empty
import org.example.eventsourcedentity
import org.example.eventsourcedentity.domain.CounterState

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
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

  def increase(command: IncreaseValue): EventSourcedResult[Empty] =
    interpretEffects(() => entity.increase(currentState, command))

  def decrease(command: DecreaseValue): EventSourcedResult[Empty] =
    interpretEffects(() => entity.decrease(currentState, command))
}
