/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import java.util.Collection;
import java.util.Optional;

/** The principals associated with a request. */
public interface Principals {
  /** Whether this request was from the internet. */
  boolean isInternet();

  /** Whether this is a self request. */
  boolean isSelf();

  /** Whether this request is a backoffice request. */
  boolean isBackoffice();

  /**
   * Whether this request was from a service in the local project.
   *
   * @param name The name of the service.
   */
  boolean isLocalService(String name);
  /** Whether this request was from any service in the local project. */
  boolean isAnyLocalService();
  /** Get the service that invoked this call, if any. */
  Optional<String> getLocalService();

  /** Get the principals associated with this request. */
  Collection<Principal> get();
}
