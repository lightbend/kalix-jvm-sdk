/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.google.protobuf.Any;

/** Represents a call to a service, performed either as a forward, or as an effect. */
public interface ServiceCall {

  /**
   * The reference to the call.
   *
   * @return The reference to the call.
   */
  ServiceCallRef<?> ref();

  /**
   * The message to pass to the call when the call is invoked.
   *
   * @return The message to pass to the call, serialized as an {@link Any}.
   */
  Any message();

  /**
   * The metadata to pass with the message when the call is invoked.
   *
   * @return The metadata.
   */
  Metadata metadata();
}
