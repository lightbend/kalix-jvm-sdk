/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.view;

public class TransformedUser {
  public final String name;
  public final String email;

  public TransformedUser(String name, String email) {
    this.name = name;
    this.email = email;
  }
}
