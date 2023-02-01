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
 *  Interface used to represent a status code, typically used when replying with an error.
 *
 */

public interface StatusCode {
  /**
   * The supported HTTP error codes that can be used when replying from the Kalix user function.
   * Note: this list is not an exhaustive list of all the possible HTTP codes but only the ones available for use currently.
   *
   */
  enum HttpErrorCode implements StatusCode {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    public final int httpCode;
    HttpErrorCode(int httpCode) {
      this.httpCode = httpCode;
    }
  }

}


