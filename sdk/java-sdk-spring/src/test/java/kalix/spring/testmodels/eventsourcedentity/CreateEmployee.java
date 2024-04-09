/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.eventsourcedentity;

public class CreateEmployee {

  public final String firstName;
  public final String lastName;

  public final String email;

  public CreateEmployee(String firstName, String lastName, String email) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }
}
