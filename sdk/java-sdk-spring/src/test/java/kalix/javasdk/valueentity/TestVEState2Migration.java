/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import kalix.javasdk.JsonMigration;


public class TestVEState2Migration extends JsonMigration {

  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion < 1) {
      return ((ObjectNode) json).set("newValue", TextNode.valueOf("newValue"));
    } else {
      return null;

    }
  }
}
