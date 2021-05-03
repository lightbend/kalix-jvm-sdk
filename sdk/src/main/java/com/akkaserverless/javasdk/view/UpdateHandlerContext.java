/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.MetadataContext;

import java.util.Optional;

/** Context for view update calls. */
public interface UpdateHandlerContext extends ViewContext, MetadataContext {

  /**
   * The origin subject of the {@link CloudEvent}. For example, the entity key when the event was
   * emitted from an entity.
   */
  Optional<String> eventSubject();

  /** The name of the command being executed. */
  String commandName();
}
