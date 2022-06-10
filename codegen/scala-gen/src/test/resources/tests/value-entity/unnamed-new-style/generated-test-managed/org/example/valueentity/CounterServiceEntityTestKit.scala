package org.example.valueentity

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.testkit.MockRegistry
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.testkit.impl.TestKitValueEntityCommandContext
import kalix.scalasdk.testkit.impl.TestKitValueEntityContext
import kalix.scalasdk.testkit.impl.ValueEntityResultImpl
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import org.example.valueentity
import org.example.valueentity.domain.CounterState

// This code is managed by Kalix tooling.
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
    apply("testkit-entity-id", entityFactory, MockRegistry.empty)

  /**
   * Create a testkit instance of CounterServiceEntity
   * @param entityFactory A function that creates a CounterServiceEntity based on the given ValueEntityContext,
   *                      a default entity id is used.
   * @param mockRegistry  A registry to be provided in cases which the entity calls other components to allow for unit testing.
   */
  def apply(entityFactory: ValueEntityContext => CounterServiceEntity, mockRegistry: MockRegistry): CounterServiceEntityTestKit =
    apply("testkit-entity-id", entityFactory, mockRegistry)

  /**
   * Create a testkit instance of CounterServiceEntity with a specific entity id.
   * @param entityId      An entity identifier
   * @param entityFactory A function that creates a CounterServiceEntity based on the given ValueEntityContext,
   *                      a default entity id is used.
   * @param mockRegistry  A registry to be provided in cases which the entity calls other components to allow for unit testing.
   */
  def apply(entityId: String, entityFactory: ValueEntityContext => CounterServiceEntity, mockRegistry: MockRegistry = MockRegistry.empty): CounterServiceEntityTestKit =
    new CounterServiceEntityTestKit(entityFactory(new TestKitValueEntityContext(entityId, mockRegistry)), entityId)
}

/**
 * TestKit for unit testing CounterServiceEntity
 */
final class CounterServiceEntityTestKit private(entity: CounterServiceEntity, entityId: String) {
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

  def increase(command: IncreaseValue, metadata: Metadata = Metadata.empty): ValueEntityResult[Empty] = {
    entity._internalSetCommandContext(Some(new TestKitValueEntityCommandContext(entityId = entityId, metadata = metadata)))
    val effect = entity.increase(state, command)
    interpretEffects(effect)
  }

  def decrease(command: DecreaseValue, metadata: Metadata = Metadata.empty): ValueEntityResult[Empty] = {
    entity._internalSetCommandContext(Some(new TestKitValueEntityCommandContext(entityId = entityId, metadata = metadata)))
    val effect = entity.decrease(state, command)
    interpretEffects(effect)
  }
}
