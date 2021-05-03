/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.MetadataContext;

/** Context for views. */
public interface ViewContext extends Context {
  /**
   * The id of the view that this context is for.
   *
   * @return The view id.
   */
  String viewId();
}
