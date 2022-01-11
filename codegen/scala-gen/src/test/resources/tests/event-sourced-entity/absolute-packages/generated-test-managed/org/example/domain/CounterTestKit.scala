package org.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedEntityEffectsRunner
import com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityCommandContext
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityEventContext
import com.google.protobuf.empty.Empty
import org.example.eventsourcedentity.counter_api
import org.example.state.counter_state.CounterState

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
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
   */
  def apply(entityFactory: EventSourcedEntityContext => Counter): CounterTestKit =
    apply("testkit-entity-id", entityFactory)
  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  def apply(entityId: String, entityFactory: EventSourcedEntityContext => Counter): CounterTestKit =
    new CounterTestKit(entityFactory(new TestKitEventSourcedEntityContext(entityId)))
}
final class CounterTestKit private(entity: Counter) extends EventSourcedEntityEffectsRunner[CounterState](entity: Counter) {

  override protected def handleEvent(state: CounterState, event: Any): CounterState = {
    event match {
      case e: org.example.events.counter_events.Increased =>
        entity.increased(state, e)

      case e: org.example.events.counter_events.Decreased =>
        entity.decreased(state, e)
    }
  }

  def increase(command: counter_api.IncreaseValue): EventSourcedResult[Empty] =
    interpretEffects(() => entity.increase(currentState, command))

  def decrease(command: counter_api.DecreaseValue): EventSourcedResult[Empty] =
    interpretEffects(() => entity.decrease(currentState, command))
}
