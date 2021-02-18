/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.action;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CallHandler;
import com.akkaserverless.tck.model.Action.OtherRequest;
import com.akkaserverless.tck.model.Action.Response;

@Action
public class ActionTwoBehavior {
  public ActionTwoBehavior() {}

  @CallHandler
  public Response call(OtherRequest request) {
    return Response.getDefaultInstance();
  }
}
