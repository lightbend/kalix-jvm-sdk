/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.view;

import kalix.javasdk.CloudEvent;
import kalix.javasdk.MetadataContext;

import java.util.Optional;

/** Context for view update calls. */
public interface UpdateContext extends ViewContext, MetadataContext {

  /**
   * The origin subject of the {@link CloudEvent}. For example, the entity id when the event was
   * emitted from an entity.
   */
  Optional<String> eventSubject();

  /** The name of the event being handled. */
  String eventName();
}
