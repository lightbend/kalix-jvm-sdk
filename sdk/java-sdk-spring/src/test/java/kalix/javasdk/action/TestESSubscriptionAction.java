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

package kalix.javasdk.action;

import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.eventsourcedentity.TestESEvent;
import kalix.javasdk.eventsourcedentity.TestEventSourcedEntity;

@Subscribe.EventSourcedEntity(value = TestEventSourcedEntity.class, ignoreUnknown = true)
public class TestESSubscriptionAction extends Action {

  public Effect<Integer> handleEvent2(TestESEvent.Event2 event) {
    return effects().reply(event.newName());
  }

  public Effect<Boolean> handleEvent3(TestESEvent.Event3 event) {
    return effects().reply(event.b());
  }

  public Effect<String> handleEvent4(TestESEvent.Event4 event) {
    return effects().reply(event.anotherString());
  }
}
