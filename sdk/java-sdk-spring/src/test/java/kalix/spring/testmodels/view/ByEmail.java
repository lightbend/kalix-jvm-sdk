/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.view;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ByEmail {

  public final String email;

  @JsonCreator
  public ByEmail(String email) {
    this.email = email;
  }
}
