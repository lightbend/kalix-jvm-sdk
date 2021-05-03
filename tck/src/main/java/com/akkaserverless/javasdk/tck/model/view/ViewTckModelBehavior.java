/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.view;

import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.UpdateHandlerContext;
import com.akkaserverless.javasdk.view.View;
import com.akkaserverless.tck.model.View.Event;
import com.akkaserverless.tck.model.View.ViewState;

import java.util.Optional;

@View
public class ViewTckModelBehavior {

  @UpdateHandler
  public Optional<ViewState> processUpdateUnary(
      Event event, Optional<ViewState> maybePreviousState, UpdateHandlerContext ctx) {
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
