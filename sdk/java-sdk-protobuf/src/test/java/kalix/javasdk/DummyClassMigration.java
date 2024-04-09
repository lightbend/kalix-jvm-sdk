/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DummyClassMigration extends JsonMigration {
  @Override
  public int currentVersion() {
    return 0;
  }

  @Override
  public int supportedForwardVersion() {
    return 1;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion == 1) {
      ObjectNode objectNode = ((ObjectNode) json);
      objectNode.set("optionalStringValue", objectNode.get("mandatoryStringValue"));
      objectNode.remove("mandatoryStringValue");
      return objectNode;
    } else {
      return json;
    }
  }
}
