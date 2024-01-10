/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * This annotation is used to mark a method as a hook that will be triggered automatically uppon certain events.
 * The method must be public and have no parameters.
 * If the call fails, it will be retried up to the number of times specified by the maxRetries parameter.
 */
public @interface Trigger {

  /**
   * The on startup hook is called every time a service instance boots up.
   * This can happen for very different reasons: restarting / redeploying the service,
   * scaling up to more instances or even without any user-driven action
   * (e.g. Kalix Runtime versions being rolled out, infrastructure-related incidents, etc.).
   * Therefore, one should carefully consider how to use this hook and its implementation.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface OnStartup {
    /**
     * The maximum number of retries we will do upon failure of the method hook calls.
     * The default value 0 means no retries are done.
     */
    int maxRetries() default 0;
  }

}
