package org.example.service

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.impl.ActionResultImpl
import kalix.scalasdk.testkit.impl.TestKitActionContext

// This code is managed by Kalix tooling.
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
  def apply(actionFactory: ActionCreationContext => MyServiceActionImpl): MyServiceActionImplTestKit =
    new MyServiceActionImplTestKit(actionFactory)

}

/**
 * TestKit for unit testing MyServiceActionImpl
 */
final class MyServiceActionImplTestKit private(actionFactory: ActionCreationContext => MyServiceActionImpl) {

  private def newActionInstance(context: TestKitActionContext) = {
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: MyRequest, metadata: Metadata = Metadata.empty): ActionResult[Empty] = {
    val context = new TestKitActionContext(metadata)
    new ActionResultImpl(newActionInstance(context).simpleMethod(command))
  }
}
