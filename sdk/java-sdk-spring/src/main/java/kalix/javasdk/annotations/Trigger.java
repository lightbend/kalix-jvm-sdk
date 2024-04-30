/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
