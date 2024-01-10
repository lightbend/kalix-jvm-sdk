/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.tck.model.view;

import kalix.javasdk.view.ViewContext;

public class ViewTckModelImpl extends AbstractViewTckModelView {

  public ViewTckModelImpl(ViewContext context) {}

  @Override
  public View.ViewState emptyState() {
    return null;
  }

  @Override
  public UpdateEffect<View.ViewState> processUpdateUnary(View.ViewState state, View.Event event) {
    if (event.hasReturnAsIs()) {
      return effects()
          .updateState(
              View.ViewState.newBuilder().setData(event.getReturnAsIs().getData()).build());
    } else if (event.hasUppercaseThis()) {
      return effects()
          .updateState(
              View.ViewState.newBuilder()
                  .setData(event.getUppercaseThis().getData().toUpperCase())
                  .build());
    } else if (event.hasAppendToExistingState()) {;
      if (state == null) throw new IllegalArgumentException("State was null for " + event);
      return effects()
          .updateState(
              state
                  .toBuilder()
                  .setData(state.getData() + event.getAppendToExistingState().getData())
                  .build());
    } else if (event.hasFail()) {
      throw new RuntimeException("Fail");
    } else if (event.hasIgnore()) {
      return effects().ignore();
    } else {
      throw new RuntimeException("Unexpected event type " + event.getEventCase().name());
    }
  }
}
