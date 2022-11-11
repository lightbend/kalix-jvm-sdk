package org.example.valueentity.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.testkit.impl.TestKitValueEntityCommandContext
import kalix.scalasdk.testkit.impl.TestKitValueEntityContext
import kalix.scalasdk.testkit.impl.ValueEntityResultImpl
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity
import org.example.valueentity.state.CounterState

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing Counter
 */
object CounterTestKit {
  /**
   * Create a testkit instance of Counter
   * @param entityFactory A function that creates a Counter based on the given ValueEntityContext,
   *                      a default entity id is used.
   */
  def apply(entityFactory: ValueEntityContext => Counter): CounterTestKit =
    apply("testkit-entity-id", entityFactory)

  /**
   * Create a testkit instance of Counter with a specific entity id.
   */
  def apply(entityId: String, entityFactory: ValueEntityContext => Counter): CounterTestKit =
    new CounterTestKit(entityFactory(new TestKitValueEntityContext(entityId)), entityId)
}

/**
 * TestKit for unit testing Counter
 */
final class CounterTestKit private(entity: Counter, entityId: String) {
  private var state: CounterState = entity.emptyState

  /**
   * @return The current state of the Counter under test
   */
  def currentState(): CounterState =
    state

  private def interpretEffects[Reply](effect: ValueEntity.Effect[Reply]): ValueEntityResult[Reply] = {
    val result = new ValueEntityResultImpl[Reply](effect)
    if (result.stateWasUpdated)
      this.state = result.updatedState.asInstanceOf[CounterState]
    else if (result.stateWasDeleted)
      this.state = entity.emptyState
    result
  }

  def increase(command: valueentity.IncreaseValue, metadata: Metadata = Metadata.empty): ValueEntityResult[Empty] = {
    entity._internalSetCommandContext(Some(new TestKitValueEntityCommandContext(entityId = entityId, metadata = metadata)))
    val effect = entity.increase(state, command)
    interpretEffects(effect)
  }

  def decrease(command: valueentity.DecreaseValue, metadata: Metadata = Metadata.empty): ValueEntityResult[Empty] = {
    entity._internalSetCommandContext(Some(new TestKitValueEntityCommandContext(entityId = entityId, metadata = metadata)))
    val effect = entity.decrease(state, command)
    interpretEffects(effect)
  }
}
