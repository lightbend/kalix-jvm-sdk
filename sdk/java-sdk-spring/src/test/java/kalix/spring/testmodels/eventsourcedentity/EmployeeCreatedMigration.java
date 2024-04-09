/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.eventsourcedentity;

import kalix.javasdk.JsonMigration;

import java.util.List;

public class EmployeeCreatedMigration extends JsonMigration {
  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public List<String> supportedClassNames() {
    return List.of("old-created");
  }
}
