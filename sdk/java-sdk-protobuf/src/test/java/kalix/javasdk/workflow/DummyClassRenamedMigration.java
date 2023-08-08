/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import kalix.javasdk.DummyClass2;
import kalix.javasdk.JacksonMigration;

import java.util.List;

public class DummyClassRenamedMigration extends JacksonMigration {
  @Override
  public int currentVersion() {
    return 1;
  }

  @Override
  public JsonNode transform(int fromVersion, JsonNode jsonNode) {
    return jsonNode;
  }

  @Override
  public List<String> supportedClassNames() {
    return List.of(DummyClass2.class.getName());
  }
}
