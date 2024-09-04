package com.example.actions

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased

// tag::counter-topic[]
class CounterJournalToTopicAction(creationContext: ActionCreationContext) extends AbstractCounterJournalToTopicAction {

  // tag::counter-topic-event-subject[]
  override def onIncreased(valueIncreased: ValueIncreased): Action.Effect[Increased] = {
    // end::counter-topic[]
    val counterId = actionContext.eventSubject // <1>
    // end::counter-topic-event-subject[]
    /*
    // tag::counter-topic-event-subject[]
    ...
    // end::counter-topic-event-subject[]
    */
    // tag::counter-topic[]
    effects.reply(Increased(valueIncreased.value)) // <1>
    // tag::counter-topic-event-subject[]
  }
  // end::counter-topic-event-subject[]
  // end::counter-topic[]

  override def onDecreased(valueDecreased: ValueDecreased): Action.Effect[Decreased] = {
    effects.reply(Decreased(valueDecreased.value))
  }
  // tag::counter-topic[]
}
// end::counter-topic[]
