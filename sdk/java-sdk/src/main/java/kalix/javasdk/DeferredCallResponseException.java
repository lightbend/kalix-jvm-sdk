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

package kalix.javasdk;

/**
 * Exception used when a DeferredCall fails to wrap the origin error, plus the error code associated.
 */
public interface DeferredCallResponseException {

  /**
   * A description of the original problem that caused the error.
   */
  String description();

  /**
   * The error code associated with the failure.
   * E.g. if the original call fails with a 404 this will return a StatusCode.NOT_FOUND.
   */
  StatusCode.ErrorCode errorCode();

  /**
   * The original exception that caused the deferred call to fail.
   */
  Throwable cause();
}


