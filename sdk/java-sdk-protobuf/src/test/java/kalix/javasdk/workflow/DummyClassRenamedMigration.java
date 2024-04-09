/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import kalix.javasdk.DummyClass2;
import kalix.javasdk.JsonMigration;

import java.util.List;

public class DummyClassRenamedMigration extends JsonMigration {
  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    return json;
  }

  @Override
  public List<String> supportedClassNames() {
    return List.of(DummyClass2.class.getName());
  }
}
