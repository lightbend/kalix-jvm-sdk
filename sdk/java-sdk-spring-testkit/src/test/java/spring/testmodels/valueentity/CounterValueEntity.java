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

package kalix.javasdk.testmodels.valueentity;

import kalix.javasdk.valueentity.ValueEntity;

public class CounterValueEntity extends ValueEntity<Integer> {

  @Override
  public Integer emptyState() {
    return 0;
  }

  public Effect<String> increaseBy(Integer value) {
    Integer state = currentState();
    if (value < 0) return effects().error("Can't increase with a negative value");
    else return effects().updateState(state + value).thenReply("Ok");
  }

  public Effect<String> increaseFromMeta() {
    Integer state = currentState();
    return effects().updateState(state + Integer.parseInt(commandContext().metadata().get("value").get())).thenReply("Ok");
  }

  public Effect<String> delete() {
    return effects().deleteEntity().thenReply("Deleted");
  }
}
