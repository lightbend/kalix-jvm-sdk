package org.example.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import org.example.Components
import org.example.ComponentsImpl
import org.example.state.CounterState
import org.example.valueentity

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractCounter extends ValueEntity[CounterState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  def increase(currentState: CounterState, increaseValue: valueentity.IncreaseValue): ValueEntity.Effect[Empty]

  def decrease(currentState: CounterState, decreaseValue: valueentity.DecreaseValue): ValueEntity.Effect[Empty]

}

