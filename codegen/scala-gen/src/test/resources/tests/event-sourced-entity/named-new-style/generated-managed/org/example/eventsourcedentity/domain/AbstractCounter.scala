package org.example.eventsourcedentity.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import org.example.Components
import org.example.ComponentsImpl
import org.example.eventsourcedentity

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractCounter extends EventSourcedEntity[CounterState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  def increase(currentState: CounterState, increaseValue: eventsourcedentity.IncreaseValue): EventSourcedEntity.Effect[Empty]

  def decrease(currentState: CounterState, decreaseValue: eventsourcedentity.DecreaseValue): EventSourcedEntity.Effect[Empty]

  def increased(currentState: CounterState, increased: Increased): CounterState
  def decreased(currentState: CounterState, decreased: Decreased): CounterState
}

