/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import java.util.Objects;

/** A principal associated with a request. */
public interface Principal {

  /** Basic principals that have no additional configuration. */
  enum Basic implements Principal {
    INTERNET,
    SELF,
    BACKOFFICE
  }

  /** A local service principal. */
  final class LocalService implements Principal {
    private final String name;

    private LocalService(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LocalService that = (LocalService) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    public String toString() {
      return "LocalService(" + name + ")";
    }
  }

  /** Abstract principal representing all requests from the internet */
  Principal INTERNET = Basic.INTERNET;
  /** Abstract principal representing all requests from self */
  Principal SELF = Basic.SELF;
  /** Abstract principal representing all requests from the backoffice */
  Principal BACKOFFICE = Basic.BACKOFFICE;

  /** Create a local service principal with the given name */
  static LocalService localService(String name) {
    return new LocalService(name);
  }
}
