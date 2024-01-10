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
