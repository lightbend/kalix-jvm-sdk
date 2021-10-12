package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.empty.Empty

/** An action. */
// tag::counter-topic[]
// tag::counter-ignore[]
class CounterJournalToTopicAction(creationContext: ActionCreationContext) extends AbstractCounterJournalToTopicAction {
// end::counter-ignore[]

  /** Handler for "Increase". */
  // tag::counter-topic-event-subject[]
  override def increase(valueIncreased: ValueIncreased): Action.Effect[Increased] = {
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

  /** Handler for "Decrease". */
  override def decrease(valueDecreased: ValueDecreased): Action.Effect[Decreased] = {
    effects.reply(Decreased(valueDecreased.value))
  }

  /** Handler for "Ignore". */
  // tag::counter-ignore[]
  override def ignore(any: ScalaPbAny): Action.Effect[Empty] =
    effects.noReply // <1>
  // tag::counter-topic[]
}
// end::counter-ignore[]
// end::counter-topic[]