/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/** Context that provides access to metadata. */
public interface MetadataContext extends Context {
  /** Get the metadata associated with this context. */
  Metadata metadata();
}
