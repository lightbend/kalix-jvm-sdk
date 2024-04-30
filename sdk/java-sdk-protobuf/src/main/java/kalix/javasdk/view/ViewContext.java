/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.view;

import kalix.javasdk.Context;

/** Context for views. */
public interface ViewContext extends Context {
  /**
   * The id of the view that this context is for.
   *
   * @return The view id.
   */
  String viewId();
}
