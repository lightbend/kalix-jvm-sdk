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

package com.akkaserverless.javasdk.tck.model.view;

import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.javasdk.view.ViewContext;
import com.akkaserverless.tck.model.View.Event;
import com.akkaserverless.tck.model.View.ViewState;

public class ViewTckModelBehavior extends View<ViewState> {

  @SuppressWarnings("unused")
  public ViewTckModelBehavior(ViewContext context) {}

  @Override
  public ViewState emptyState() {
    return null;
  }

  public View.UpdateEffect<ViewState> processUpdateUnary(ViewState state, Event event) {
    if (event.hasReturnAsIs()) {
      return updateEffects()
          .updateState(ViewState.newBuilder().setData(event.getReturnAsIs().getData()).build());
    } else if (event.hasUppercaseThis()) {
      return updateEffects()
          .updateState(
              ViewState.newBuilder()
                  .setData(event.getUppercaseThis().getData().toUpperCase())
                  .build());
    } else if (event.hasAppendToExistingState()) {;
      if (state == null) throw new IllegalArgumentException("State was null for " + event);
      return updateEffects()
          .updateState(
              state
                  .toBuilder()
                  .setData(state.getData() + event.getAppendToExistingState().getData())
                  .build());
    } else if (event.hasFail()) {
      throw new RuntimeException("Fail");
    } else if (event.hasIgnore()) {
      return updateEffects().ignore();
    } else {
      throw new RuntimeException("Unexpected event type " + event.getEventCase().name());
    }
  }
}
