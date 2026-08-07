/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.view

import java.util.Optional

import akka.stream.Materializer
import kalix.javasdk.Metadata
import kalix.javasdk.view.{ UpdateContext, View }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ViewRouterSpec extends AnyWordSpec with Matchers {

  private val context: UpdateContext = new UpdateContext {
    override def eventSubject(): Optional[String] = Optional.empty()
    override def eventName(): String = "SomeEvent"
    override def viewId(): String = "view-id"
    override def metadata(): Metadata = Metadata.EMPTY
    override def materializer(): Materializer = null
  }

  class TestView extends View[String] {
    var viewStateSeenInHandler: String = _

    def handleEvent(state: String, event: Any): View.UpdateEffect[String] = {
      viewStateSeenInHandler = viewState()
      effects().updateState(state)
    }

    def viewStateAvailable(): Boolean =
      try {
        viewState()
        true
      } catch {
        case _: IllegalStateException => false
      }
  }

  class TestViewRouter(view: TestView) extends ViewRouter[String, TestView](view) {
    override def handleUpdate(commandName: String, state: String, event: Any): View.UpdateEffect[String] =
      view.handleEvent(state, event)
  }

  "ViewRouter" should {

    "make the current view state available via View.viewState() while handling an update" in {
      val view = new TestView
      val router = new TestViewRouter(view)

      router._internalHandleUpdate(Some("previous-state"), "some-event", context)

      view.viewStateSeenInHandler shouldBe "previous-state"
    }

    "make the empty state available via View.viewState() when there is no previous state" in {
      val view = new TestView {
        override def emptyState(): String = "empty-state"
      }
      val router = new TestViewRouter(view)

      router._internalHandleUpdate(None, "some-event", context)

      view.viewStateSeenInHandler shouldBe "empty-state"
    }

    "make View.viewState() unavailable again once update handling has completed" in {
      val view = new TestView
      val router = new TestViewRouter(view)

      router._internalHandleUpdate(Some("previous-state"), "some-event", context)

      view.viewStateAvailable() shouldBe false
    }
  }
}
