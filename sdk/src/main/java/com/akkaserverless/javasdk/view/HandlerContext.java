/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.MetadataContext;

import java.util.Optional;

/** Context for view update calls. */
public interface HandlerContext extends ViewContext, MetadataContext {

  /** The entity that emitted the event. */
  Optional<String> sourceEntityId();

  /** The name of the command being executed. */
  String commandName();
}
