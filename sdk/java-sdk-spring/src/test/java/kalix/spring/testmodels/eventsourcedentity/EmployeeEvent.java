/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.eventsourcedentity;

import kalix.javasdk.annotations.Migration;
import kalix.javasdk.annotations.TypeName;

public sealed interface EmployeeEvent {

  @TypeName("created")
  @Migration(EmployeeCreatedMigration.class)
  final class EmployeeCreated implements EmployeeEvent {

    public final String firstName;
    public final String lastName;
    public final String email;

    public EmployeeCreated(String firstName, String lastName, String email) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
    }
  }

  @TypeName("emailUpdated")
  final class EmployeeEmailUpdated implements EmployeeEvent {

    public final String email;

    public EmployeeEmailUpdated(String email) {
      this.email = email;
    }
  }

}
