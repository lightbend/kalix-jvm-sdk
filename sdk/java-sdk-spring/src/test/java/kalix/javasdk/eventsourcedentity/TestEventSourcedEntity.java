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

import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("es")
@RequestMapping("/es")
public class TestEventSourcedEntity extends EventSourcedEntity<TestESState, TestESEvent> {

  @Override
  public TestESState emptyState() {
    return new TestESState("", 0, false, "");
  }

  @GetMapping
  public Effect<TestESState> get() {
    return effects().reply(currentState());
  }

  @EventHandler
  public TestESState apply(TestESEvent.Event1 event1) {
    return new TestESState(event1.s(), currentState().i(), currentState().b(), currentState().anotherString());
  }

  @EventHandler
  public TestESState apply(TestESEvent.Event2 event2) {
    return new TestESState(currentState().s(), event2.newName(), currentState().b(), currentState().anotherString());
  }

  @EventHandler
  public TestESState apply(TestESEvent.Event3 event3) {
    return new TestESState(currentState().s(), currentState().i(), event3.b(), currentState().anotherString());
  }

  @EventHandler
  public TestESState apply(TestESEvent.Event4 event4) {
    return new TestESState(currentState().s(), currentState().i(), currentState().b(), event4.anotherString());
  }
}
