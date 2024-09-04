package com.example.actions

import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::class[]
class CounterJournalToTopicWithMetaAction(creationContext: ActionCreationContext)
    extends AbstractCounterJournalToTopicWithMetaAction {

  override def onIncreased(valueIncreased: ValueIncreased): Action.Effect[Increased] = {
    val increased = Increased(valueIncreased.value)
    val counterId = actionContext.metadata.get("ce-subject").get // <1>
    val metadata = Metadata.empty.add("ce-subject", counterId)
    effects.reply(increased, metadata) // <2>
  }
  // end::class[]

  override def onDecreased(valueDecreased: ValueDecreased): Action.Effect[Decreased] = {
    val decreased = Decreased(valueDecreased.value)
    val counterId = actionContext.metadata.get("ce-subject").get // <1>
    val metadata = Metadata.empty.add("ce-subject", counterId)
    effects.reply(decreased, metadata)
  }
  // tag::class[]
}
// end::class[]
