/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
