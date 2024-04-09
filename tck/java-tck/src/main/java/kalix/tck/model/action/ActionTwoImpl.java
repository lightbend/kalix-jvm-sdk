/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.action;

import kalix.javasdk.action.ActionCreationContext;
import kalix.tck.model.action.Action.*;

import java.util.concurrent.CompletableFuture;

public class ActionTwoImpl extends AbstractActionTwoAction {

  public ActionTwoImpl(ActionCreationContext creationContext) {}

  @Override
  public Effect<Response> call(OtherRequest request) {
    return effects().asyncReply(CompletableFuture.completedFuture(Response.getDefaultInstance()));
  }
}
