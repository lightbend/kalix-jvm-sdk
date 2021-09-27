/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import com.google.protobuf.any.Any
import com.google.protobuf.empty.Empty

/** An action. */
class CounterJournalToTopicAction(creationContext: ActionCreationContext) extends AbstractCounterJournalToTopicAction {

  /** Handler for "Increase". */
  override def increase(valueIncreased: ValueIncreased): Action.Effect[Increased] = {
    throw new RuntimeException("The command handler for `Increase` is not implemented, yet")
  }
  /** Handler for "Decrease". */
  override def decrease(valueDecreased: ValueDecreased): Action.Effect[Decreased] = {
    throw new RuntimeException("The command handler for `Decrease` is not implemented, yet")
  }
  /** Handler for "Ignore". */
  override def ignore(any: Any): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `Ignore` is not implemented, yet")
  }
}
