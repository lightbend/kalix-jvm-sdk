package org.example.eventsourcedentity.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.google.protobuf.empty.Empty
import org.example.Components
import org.example.ComponentsImpl
import org.example.eventsourcedentity.counter_api
import org.example.eventsourcedentity.events.counter_events.Decreased
import org.example.eventsourcedentity.events.counter_events.Increased
import org.example.eventsourcedentity.state.counter_state.CounterState

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An event sourced entity. */
abstract class AbstractCounter extends EventSourcedEntity[CounterState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  def increase(currentState: CounterState, increaseValue: counter_api.IncreaseValue): EventSourcedEntity.Effect[Empty]

  def decrease(currentState: CounterState, decreaseValue: counter_api.DecreaseValue): EventSourcedEntity.Effect[Empty]

  def increased(currentState: CounterState, increased: Increased): CounterState
  def decreased(currentState: CounterState, decreased: Decreased): CounterState
}

