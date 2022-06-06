package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.testkit.ActionResult
import kalix.scalasdk.testkit.MockRegistry
import kalix.scalasdk.testkit.impl.ActionResultImpl
import kalix.scalasdk.testkit.impl.TestKitActionContext
import org.external.Empty

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing MyServiceAction
 */
object MyServiceActionTestKit {
  /**
   * Create a testkit instance of MyServiceAction
   * @param entityFactory A function that creates a MyServiceAction based on the given ActionCreationContext
   * @param mockRegistry A map of mocks (Class -> mock) that provides control and the ability to test the dependencies on another components / services
   */
  def apply(actionFactory: ActionCreationContext => MyServiceAction, mockRegistry: MockRegistry = MockRegistry.empty): MyServiceActionTestKit =
    new MyServiceActionTestKit(actionFactory, mockRegistry)

}

/**
 * TestKit for unit testing MyServiceAction
 */
final class MyServiceActionTestKit private(actionFactory: ActionCreationContext => MyServiceAction, mockRegistry: MockRegistry) {

  private def newActionInstance(context: TestKitActionContext) = {
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: MyRequest, metadata: Metadata = Metadata.empty): ActionResult[Empty] = {
    val context = new TestKitActionContext(metadata, mockRegistry)
    new ActionResultImpl(newActionInstance(context).simpleMethod(command))
  }

  def streamedOutputMethod(command: MyRequest, metadata: Metadata = Metadata.empty): Source[ActionResult[Empty], akka.NotUsed] = {
    val context = new TestKitActionContext(metadata, mockRegistry)
    newActionInstance(context).streamedOutputMethod(command).map(effect => new ActionResultImpl(effect))
  }

  def streamedInputMethod(command: Source[MyRequest, akka.NotUsed], metadata: Metadata = Metadata.empty): ActionResult[Empty] = {
    val context = new TestKitActionContext(metadata, mockRegistry)
    new ActionResultImpl(newActionInstance(context).streamedInputMethod(command))
  }

  def fullStreamedMethod(command: Source[MyRequest, akka.NotUsed], metadata: Metadata = Metadata.empty): Source[ActionResult[Empty], akka.NotUsed] = {
    val context = new TestKitActionContext(metadata, mockRegistry)
    newActionInstance(context).fullStreamedMethod(command).map(effect => new ActionResultImpl(effect))
  }
}
