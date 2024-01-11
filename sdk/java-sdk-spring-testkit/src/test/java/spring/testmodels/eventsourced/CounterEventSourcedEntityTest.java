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

import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterEventSourcedEntityTest {

  @Test
  public void testIncrease() {
    EventSourcedTestKit<Integer, Increased, CounterEventSourcedEntity> testKit =
        EventSourcedTestKit.of(ctx -> new CounterEventSourcedEntity());
    EventSourcedResult<String> result = testKit.call(entity -> entity.increaseBy(10));
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Ok");
    assertEquals(testKit.getState(), 10);
    assertEquals(testKit.getAllEvents().size(), 1);
  }

  @Test
  public void testIncreaseWithMetadata() {
    EventSourcedTestKit<Integer, Increased, CounterEventSourcedEntity> testKit =
        EventSourcedTestKit.of(ctx -> new CounterEventSourcedEntity());
    EventSourcedResult<String> result = testKit.call(entity -> entity.increaseFromMeta(), Metadata.EMPTY.add("value", "10"));
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Ok");
    assertEquals(testKit.getState(), 10);
    assertEquals(testKit.getAllEvents().size(), 1);
  }

  @Test
  public void testDoubleIncrease() {
    EventSourcedTestKit<Integer, Increased, CounterEventSourcedEntity> testKit =
        EventSourcedTestKit.of(ctx -> new CounterEventSourcedEntity());
    EventSourcedResult<String> result = testKit.call(entity -> entity.doubleIncreaseBy(10));
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Ok");
    assertEquals(testKit.getState(), 20);
    assertEquals(testKit.getAllEvents().size(), 2);
  }

  @Test
  public void testIncreaseWithNegativeValue() {
    EventSourcedTestKit<Integer, Increased, CounterEventSourcedEntity> testKit =
        EventSourcedTestKit.of(ctx -> new CounterEventSourcedEntity());
    EventSourcedResult<String> result = testKit.call(entity -> entity.increaseBy(-10));
    assertTrue(result.isError());
    assertEquals(result.getError(), "Can't increase with a negative value");
  }
}
