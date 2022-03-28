package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.impl.ActionResultImpl
import kalix.scalasdk.testkit.impl.TestKitActionContext
import org.external.Empty

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing MyServiceAction
 */
object MyServiceActionTestKit {
  /**
   * Create a testkit instance of MyServiceAction
   * @param entityFactory A function that creates a MyServiceAction based on the given ActionCreationContext
   */
  def apply(actionFactory: ActionCreationContext => MyServiceAction): MyServiceActionTestKit =
    new MyServiceActionTestKit(actionFactory)

}

/**
 * TestKit for unit testing MyServiceAction
 */
final class MyServiceActionTestKit private(actionFactory: ActionCreationContext => MyServiceAction) {

  private def newActionInstance() = {
    val context = new TestKitActionContext
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: MyRequest): ActionResult[Empty] =
    new ActionResultImpl(newActionInstance().simpleMethod(command))

  def streamedOutputMethod(command: MyRequest): Source[ActionResult[Empty], akka.NotUsed] =
    newActionInstance().streamedOutputMethod(command).map(effect => new ActionResultImpl(effect))

  def streamedInputMethod(command: Source[MyRequest, akka.NotUsed]): ActionResult[Empty] =
    new ActionResultImpl(newActionInstance().streamedInputMethod(command))

  def fullStreamedMethod(command: Source[MyRequest, akka.NotUsed]): Source[ActionResult[Empty], akka.NotUsed] =
    newActionInstance().fullStreamedMethod(command).map(effect => new ActionResultImpl(effect))
}
