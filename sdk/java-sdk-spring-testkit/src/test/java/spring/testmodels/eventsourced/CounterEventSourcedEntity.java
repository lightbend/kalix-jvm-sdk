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

package kalix.javasdk.testmodels.eventsourced;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.annotations.EventHandler;

import java.util.List;

public class CounterEventSourcedEntity extends EventSourcedEntity<Integer, Increased> {

  public Effect<String> increaseBy(Integer value) {
    if (value <= 0) return effects().error("Can't increase with a negative value");
    else return effects().emitEvent(new Increased(value)).thenReply(__ -> "Ok");
  }

  public Effect<String> increaseFromMeta() {
    return effects().emitEvent(new Increased(Integer.parseInt(commandContext().metadata().get("value").get()))).thenReply(__ -> "Ok");
  }

  public Effect<String> doubleIncreaseBy(Integer value) {
    if (value < 0) return effects().error("Can't increase with a negative value");
    else {
      Increased event = new Increased(value);
      return effects().emitEvents(List.of(event, event)).thenReply(__ -> "Ok");
    }
  }

  @EventHandler
  public Integer onEvent(Increased increased) {
    if (currentState() == null) return increased.value;
    else return currentState() + increased.value;
  }
}
