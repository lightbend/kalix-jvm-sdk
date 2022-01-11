package org.example.valueentity.counter_api

import com.akkaserverless.scalasdk.testkit.ValueEntityResult
import com.akkaserverless.scalasdk.testkit.impl.TestKitValueEntityContext
import com.akkaserverless.scalasdk.testkit.impl.ValueEntityResultImpl
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.google.protobuf.empty.Empty
import org.example.valueentity.counter_api
import org.example.valueentity.domain.counter_domain.CounterState

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing CounterServiceEntity
 */
object CounterServiceEntityTestKit {
  /**
   * Create a testkit instance of CounterServiceEntity
   * @param entityFactory A function that creates a CounterServiceEntity based on the given ValueEntityContext,
   *                      a default entity id is used.
   */
  def apply(entityFactory: ValueEntityContext => CounterServiceEntity): CounterServiceEntityTestKit =
    apply("testkit-entity-id", entityFactory)

  /**
   * Create a testkit instance of CounterServiceEntity with a specific entity id.
   */
  def apply(entityId: String, entityFactory: ValueEntityContext => CounterServiceEntity): CounterServiceEntityTestKit =
    new CounterServiceEntityTestKit(entityFactory(new TestKitValueEntityContext(entityId)))
}

/**
 * TestKit for unit testing CounterServiceEntity
 */
final class CounterServiceEntityTestKit private(entity: CounterServiceEntity) {
  private var state: CounterState = entity.emptyState

  /**
   * @return The current state of the CounterServiceEntity under test
   */
  def currentState(): CounterState =
    state

  private def interpretEffects[Reply](effect: ValueEntity.Effect[Reply]): ValueEntityResult[Reply] = {
    val result = new ValueEntityResultImpl[Reply](effect)
    if (result.stateWasUpdated)
      this.state = result.updatedState.asInstanceOf[CounterState]
    result
  }

  def increase(command: IncreaseValue): ValueEntityResult[Empty] = {
    val effect = entity.increase(state, command)
    interpretEffects(effect)
  }

  def decrease(command: DecreaseValue): ValueEntityResult[Empty] = {
    val effect = entity.decrease(state, command)
    interpretEffects(effect)
  }
}
