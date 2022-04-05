package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.akkaserverless.scalasdk.testkit.impl.ActionResultImpl
import com.akkaserverless.scalasdk.testkit.impl.TestKitActionContext
import com.google.protobuf.empty.Empty

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * TestKit for unit testing MyServiceNamedAction
 */
object MyServiceNamedActionTestKit {
  /**
   * Create a testkit instance of MyServiceNamedAction
   * @param entityFactory A function that creates a MyServiceNamedAction based on the given ActionCreationContext
   */
  def apply(actionFactory: ActionCreationContext => MyServiceNamedAction): MyServiceNamedActionTestKit =
    new MyServiceNamedActionTestKit(actionFactory)

}

/**
 * TestKit for unit testing MyServiceNamedAction
 */
final class MyServiceNamedActionTestKit private(actionFactory: ActionCreationContext => MyServiceNamedAction) {

  private def newActionInstance() = {
    val action = actionFactory(context)
    action._internalSetActionContext(Some(context))
    action
  }

  def simpleMethod(command: MyRequest, metadata: Metadata = Metadata.empty, eventSubject: Option[String] = Some("test-subject-id")): ActionResult[Empty] = {
    val context = new TestKitActionContext(metadata, eventSubject)
    new ActionResultImpl(newActionInstance(context).simpleMethod(command))
  }

  def streamedOutputMethod(command: MyRequest, metadata: Metadata = Metadata.empty, eventSubject: Option[String] = Some("test-subject-id")): Source[ActionResult[Empty], akka.NotUsed] =
    val context = new TestKitActionContext(metadata, eventSubject)
    newActionInstance(context).streamedOutputMethod(command).map(effect => new ActionResultImpl(effect))
  }

  def streamedInputMethod(command: Source[MyRequest, akka.NotUsed], metadata: Metadata = Metadata.empty, eventSubject: Option[String] = Some("test-subject-id")): ActionResult[Empty] =
    val context = new TestKitActionContext(metadata, eventSubject)
    new ActionResultImpl(newActionInstance(context).streamedInputMethod(command))
  }

  def fullStreamedMethod(command: Source[MyRequest, akka.NotUsed] metadata: Metadata = Metadata.empty, eventSubject: Option[String] = Some("test-subject-id")): Source[ActionResult[Empty], akka.NotUsed] =
    val context = new TestKitActionContext(metadata, eventSubject)
    newActionInstance(context).fullStreamedMethod(command).map(effect => new ActionResultImpl(effect))
  }
}
