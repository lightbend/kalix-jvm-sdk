package org.example.valueentity

import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.google.protobuf.empty.Empty
import org.example.valueentity

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A value entity. */
class Counter(context: ValueEntityContext) extends AbstractCounter {
  override def emptyState: CounterState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")

  override def increase(currentState: CounterState, increaseValue: IncreaseValue): ValueEntity.Effect[Empty] =
    effects.error("The command handler for `Increase` is not implemented, yet")

  override def decrease(currentState: CounterState, decreaseValue: DecreaseValue): ValueEntity.Effect[Empty] =
    effects.error("The command handler for `Decrease` is not implemented, yet")

}

