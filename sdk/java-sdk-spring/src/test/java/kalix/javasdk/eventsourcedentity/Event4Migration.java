/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import kalix.javasdk.JsonMigration;

import java.util.List;

public class Event4Migration extends JsonMigration {
  @Override
  public int currentVersion() {
    return 2;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode json) {
    if (fromVersion < 2) {
      TextNode s = (TextNode) json.get("anotherString");
      return ((ObjectNode) json).set("anotherString", TextNode.valueOf(s.textValue() + "-v2"));
    } else {
      return json;
    }
  }
}
