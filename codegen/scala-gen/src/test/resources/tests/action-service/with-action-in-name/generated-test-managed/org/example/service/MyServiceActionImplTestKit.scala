package org.example.service

import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.akkaserverless.scalasdk.testkit.impl.ActionResultImpl
import com.akkaserverless.scalasdk.testkit.impl.TestKitActionContext
import com.google.protobuf.empty.Empty

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing MyServiceActionImpl
 */
object MyServiceActionImplTestKit {
  /**
   * Create a testkit instance of MyServiceActionImpl
   * @param entityFactory A function that creates a MyServiceActionImpl based on the given ActionCreationContext
   */
  def apply(actionFactory: ActionCreationContext => MyServiceActionImpl, context: TestKitActionContext = new TestKitActionContext): MyServiceActionImplTestKit =
    new MyServiceActionImplTestKit(actionFactory, context)

}

/**
 * TestKit for unit testing MyServiceActionImpl
 */
final class MyServiceActionImplTestKit private(actionFactory: ActionCreationContext => MyServiceActionImpl, context: TestKitActionContext) {

  private def newActionInstance() = {
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: MyRequest): ActionResult[Empty] =
    new ActionResultImpl(newActionInstance().simpleMethod(command))
}
