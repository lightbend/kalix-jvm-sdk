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
