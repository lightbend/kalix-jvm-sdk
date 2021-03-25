/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.crdt;

import com.akkaserverless.javasdk.crdt.CommandHandler;
import com.akkaserverless.javasdk.crdt.CrdtEntity;
import com.akkaserverless.tck.model.CrdtEntity.Request;
import com.akkaserverless.tck.model.CrdtEntity.Response;

@CrdtEntity
public class CrdtConfiguredEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
