/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.samples.pingpong;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.pingpong.Pingpong;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerEventSourcedEntity(
            PingPongEntity.class,
            Pingpong.getDescriptor().findServiceByName("PingPongService"),
            Pingpong.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
