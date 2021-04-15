/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.replicatedentity;

import com.akkaserverless.javasdk.replicatedentity.CommandHandler;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.tck.model.ReplicatedEntity.Request;
import com.akkaserverless.tck.model.ReplicatedEntity.Response;

@ReplicatedEntity
public class ConfiguredReplicatedEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
