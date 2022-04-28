package org.example.valueentity

import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import org.example.Components
import org.example.ComponentsImpl
import org.example.valueentity
import org.example.valueentity.domain.CounterState

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractCounterServiceEntity extends ValueEntity[CounterState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  def increase(currentState: CounterState, increaseValue: IncreaseValue): ValueEntity.Effect[Empty]

  def decrease(currentState: CounterState, decreaseValue: DecreaseValue): ValueEntity.Effect[Empty]

}

