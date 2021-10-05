package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
class CounterJournalToTopicAction(creationContext: ActionCreationContext) extends AbstractCounterJournalToTopicAction {

  /** Handler for "Increase". */
  override def increase(valueIncreased: ValueIncreased): Action.Effect[Increased] = {
    effects.reply(Increased(valueIncreased.value))
  }

  /** Handler for "Decrease". */
  override def decrease(valueDecreased: ValueDecreased): Action.Effect[Decreased] = {
    effects.reply(Decreased(valueDecreased.value))
  }

  /** Handler for "Ignore". */
  override def ignore(any: ScalaPbAny): Action.Effect[Empty] = {
    effects.noReply
  }
}
