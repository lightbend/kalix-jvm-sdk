/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.action;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.akkaserverless.tck.model.Action.OtherRequest;
import com.akkaserverless.tck.model.Action.Response;

@Action
public class ActionTwoBehavior {
  public ActionTwoBehavior() {}

  @CommandHandler
  public Response call(OtherRequest request) {
    return Response.getDefaultInstance();
  }
}
