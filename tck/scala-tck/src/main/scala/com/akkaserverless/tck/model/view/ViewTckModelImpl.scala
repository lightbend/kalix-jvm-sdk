/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.tck.model.view

import com.akkaserverless.scalasdk.view.View.UpdateEffect
import com.akkaserverless.scalasdk.view.ViewContext

class ViewTckModelImpl(context: ViewContext) extends AbstractViewTckModelView {

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
    }
}
