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

import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.UpdateContext;
import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.tck.model.View.Event;
import com.akkaserverless.tck.model.View.ViewState;

import java.util.Optional;

@View
public class ViewTckModelBehavior {

  @UpdateHandler
  public Optional<ViewState> processUpdateUnary(
      Event event, Optional<ViewState> maybePreviousState, UpdateContext ctx) {
    if (event.hasReturnAsIs()) {
      return Optional.of(ViewState.newBuilder().setData(event.getReturnAsIs().getData()).build());
    } else if (event.hasUppercaseThis()) {
      return Optional.of(
          ViewState.newBuilder().setData(event.getUppercaseThis().getData().toUpperCase()).build());
    } else if (event.hasAppendToExistingState()) {
      ViewState state = maybePreviousState.get();
      return Optional.of(
          state
              .toBuilder()
              .setData(state.getData() + event.getAppendToExistingState().getData())
              .build());
    } else if (event.hasFail()) {
      throw new RuntimeException("Fail");
    } else if (event.hasIgnore()) {
      return Optional.empty();
    } else {
      throw new RuntimeException("Unexpected event type " + event.getEventCase().name());
    }
  }
}
