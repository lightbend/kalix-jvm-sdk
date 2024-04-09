/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
