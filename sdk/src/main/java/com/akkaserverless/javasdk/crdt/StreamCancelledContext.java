/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

import com.akkaserverless.javasdk.EffectContext;
import com.akkaserverless.javasdk.MetadataContext;

import java.util.function.Consumer;

/**
 * Context for a stream cancelled event.
 *
 * <p>This is sent to callbacks registered by {@link StreamedCommandContext#onCancel(Consumer)}.
 */
public interface StreamCancelledContext extends CrdtContext, EffectContext, MetadataContext {
  /**
   * The id of the command that the stream was for.
   *
   * @return The ID of the command.
   */
  long commandId();
}
