/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.MetadataContext;

import java.util.Optional;

/** Context for action calls. */
public interface ActionContext extends MetadataContext {
  /**
   * Get the metadata associated with this call.
   *
   * <p>Note, this only returns call level associated metadata. For unary in calls, this will be the
   * same as the message metadata, but for streamed calls, it will contain metadata associated with
   * the whole stream, so for example if this was a gRPC call, it will contain the HTTP headers for
   * that gRPC call.
   *
   * @return The call level metadata.
   */
  Metadata metadata();

  /**
   * The origin subject of the {@link CloudEvent}. For example, the entity key when the event was
   * emitted from an entity.
   */
  Optional<String> eventSubject();
}
