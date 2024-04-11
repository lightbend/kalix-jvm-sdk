/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.view

import kalix.scalasdk.view.View.UpdateEffect

class ViewTckModelImpl extends AbstractViewTckModelView {

  override def emptyState: ViewState = ViewState.defaultInstance

  override def processUpdateUnary(state: ViewState, event: Event): UpdateEffect[ViewState] =
    event.event match {
      case Event.Event.ReturnAsIs(ReturnAsIsEvent(data, _))       => effects.updateState(ViewState(data))
      case Event.Event.UppercaseThis(UppercaseThisEvent(data, _)) => effects.updateState(ViewState(data.toUpperCase))
      case Event.Event.AppendToExistingState(_) if state eq null =>
        throw new IllegalArgumentException("State was null for " + event)
      case Event.Event.AppendToExistingState(AppendToExistingState(data, _)) =>
        effects.updateState(ViewState(state.data + data))
      case Event.Event.Fail(_)   => effects.error("Fail")
      case Event.Event.Ignore(_) => effects.ignore()
      case Event.Event.Empty     => effects.ignore()
    }
}
