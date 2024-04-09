/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kalix.javasdk.JsonMigration;

import java.util.List;

public class Event2Migration extends JsonMigration {
  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public List<String> supportedClassNames() {
    return List.of(OldTestESEvent.OldEvent2.class.getName());
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode jsonNode) {
    if (fromVersion < 1) {
      ObjectNode objectNode = (ObjectNode) jsonNode;
      objectNode.set("newName", IntNode.valueOf(321));
      objectNode.remove("i");
      return objectNode;
    } else {
      return jsonNode;
    }
  }
}
