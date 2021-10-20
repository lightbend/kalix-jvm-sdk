/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk;

/**
 * A service call factory.
 *
 * <p>This is used to create {@link ServiceCall}'s that can be passed to <code>forward</code> of the
 * entity <code>Effect</code></code>.
 */
public interface ServiceCallFactory {

  /**
   * Lookup a reference to the service call with the given name and method.
   *
   * @param serviceName The fully qualified name of a gRPC service that this stateful service
   *     serves.
   * @param methodName The name of a method on the gRPC service.
   * @param messageType The expected type of the input message to the method.
   * @param <T> The type of the parameter that it accepts.
   * @param <R> The type of the parameter that it returns
   * @return A reference to the service call.
   * @throws java.util.NoSuchElementException if the service or method is not found.
   * @throws IllegalArgumentException if the accepted input type for the method doesn't match <code>
   *     messageType</code>.
   */
  // FIXME unsafe, arbitrary return type, deprecate or remove
  <T, R> ServiceCallRef<T, R> lookup(String serviceName, String methodName, Class<T> messageType);
}
