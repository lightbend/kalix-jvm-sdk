/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.google.protobuf.Descriptors;

/**
 * A reference to a call on a service.
 *
 * @param <T> The type of message the call accepts.
 */
public interface ServiceCallRef<T> {
  /**
   * The protobuf descriptor for the method.
   *
   * @return The protobuf descriptor for the method.
   */
  Descriptors.MethodDescriptor method();

  /**
   * Create a call from this reference, using the given message as the message to pass to it when
   * it's invoked.
   *
   * @param message The message to pass to the method.
   * @return A service call that can be used as a forward or effect.
   */
  default ServiceCall createCall(T message) {
    return createCall(message, Metadata.EMPTY);
  }

  /**
   * Create a call from this reference, using the given message as the message to pass to it when
   * it's invoked.
   *
   * @param message The message to pass to the method.
   * @param metadata The Metadata to send.
   * @return A service call that can be used as a forward or effect.
   */
  ServiceCall createCall(T message, Metadata metadata);
}
