package com.example.actions

import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.akkaserverless.scalasdk.testkit.impl.TestKitActionContext
import com.akkaserverless.scalasdk.testkit.impl.ActionResultImpl
import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import com.google.protobuf.empty.Empty

/**
 * TestKit for unit testing CounterJournalToTopicAction
 */
object CounterJournalToTopicActionTestKit {
  /**
   * Create a testkit instance of CounterJournalToTopicAction
   * @param entityFactory A function that creates a CounterJournalToTopicAction based on the given ActionCreationContext,
   *                      a default entity id is used.
   */
  def apply(actionFactory: ActionCreationContext => CounterJournalToTopicAction): CounterJournalToTopicActionTestKit =
    new CounterJournalToTopicActionTestKit(actionFactory)
}

/**
 * TestKit for unit testing CounterJournalToTopicAction
 */
class CounterJournalToTopicActionTestKit private(actionFactory: ActionCreationContext => CounterJournalToTopicAction) {

  private def newActionInstance() = actionFactory(new TestKitActionContext)

  def increase(valueIncreased: ValueIncreased): ActionResult[Increased] =
    new ActionResultImpl(newActionInstance().increase(valueIncreased))

  def decrease(valueDecreased: ValueDecreased): ActionResult[Decreased] =
    new ActionResultImpl(newActionInstance().decrease(valueDecreased))

  def ignore(any: com.google.protobuf.any.Any): ActionResult[Empty] =
    new ActionResultImpl(newActionInstance().ignore(any))

}