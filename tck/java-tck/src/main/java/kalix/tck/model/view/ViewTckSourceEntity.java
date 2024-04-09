/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.view;

import kalix.javasdk.valueentity.ValueEntityContext;

/** A value entity. */
public class ViewTckSourceEntity extends AbstractViewTckSourceEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public ViewTckSourceEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public View.Ignore emptyState() {
    return null;
  }
}
