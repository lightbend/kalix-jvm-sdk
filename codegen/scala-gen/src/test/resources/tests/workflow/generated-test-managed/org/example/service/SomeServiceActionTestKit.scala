package org.example.service

import com.google.protobuf.empty.Empty
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.MockRegistry
import kalix.scalasdk.testkit.impl.ActionResultImpl
import kalix.scalasdk.testkit.impl.TestKitActionContext

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing SomeServiceAction
 */
object SomeServiceActionTestKit {
  /**
   * Create a testkit instance of SomeServiceAction
   * @param entityFactory A function that creates a SomeServiceAction based on the given ActionCreationContext
   * @param mockRegistry A map of mocks (Class -> mock) that provides control and the ability to test the dependencies on another components / services
   */
  def apply(actionFactory: ActionCreationContext => SomeServiceAction, mockRegistry: MockRegistry = MockRegistry.empty): SomeServiceActionTestKit =
    new SomeServiceActionTestKit(actionFactory, mockRegistry)

}

/**
 * TestKit for unit testing SomeServiceAction
 */
final class SomeServiceActionTestKit private(actionFactory: ActionCreationContext => SomeServiceAction, mockRegistry: MockRegistry) {

  private def newActionInstance(context: TestKitActionContext) = {
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: SomeRequest, metadata: Metadata = Metadata.empty): ActionResult[Empty] = {
    val context = new TestKitActionContext(metadata, mockRegistry)
    new ActionResultImpl(newActionInstance(context).simpleMethod(command))
  }
}
