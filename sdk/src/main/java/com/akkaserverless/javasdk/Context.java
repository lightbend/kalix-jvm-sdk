/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/** Root class of all contexts. */
public interface Context {
  /** Get the service call factory for this stateful service. */
  ServiceCallFactory serviceCallFactory();
}
