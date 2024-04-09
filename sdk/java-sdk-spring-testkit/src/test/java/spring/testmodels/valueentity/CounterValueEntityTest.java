/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testmodels.valueentity;

import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterValueEntityTest {

  @Test
  public void testIncrease() {
    ValueEntityTestKit<Integer, CounterValueEntity> testKit =
        ValueEntityTestKit.of(ctx -> new CounterValueEntity());
    ValueEntityResult<String> result = testKit.call(entity -> entity.increaseBy(10));
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Ok");
    assertEquals(testKit.getState(), 10);
  }

  @Test
  public void testIncreaseWithMetadata() {
    ValueEntityTestKit<Integer, CounterValueEntity> testKit =
        ValueEntityTestKit.of(ctx -> new CounterValueEntity());
    ValueEntityResult<String> result = testKit.call(entity -> entity.increaseFromMeta(), Metadata.EMPTY.add("value", "10"));
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Ok");
    assertEquals(testKit.getState(), 10);
  }

  @Test
  public void testIncreaseWithNegativeValue() {
    ValueEntityTestKit<Integer, CounterValueEntity> testKit =
        ValueEntityTestKit.of(ctx -> new CounterValueEntity());
    ValueEntityResult<String> result = testKit.call(entity -> entity.increaseBy(-10));
    assertTrue(result.isError());
    assertEquals(result.getError(), "Can't increase with a negative value");
  }

  @Test
  public void testDeleteValueEntity() {
    ValueEntityTestKit<Integer, CounterValueEntity> testKit =
        ValueEntityTestKit.of(ctx -> new CounterValueEntity());
    testKit.call(entity -> entity.increaseBy(10));
    ValueEntityResult<String> result = testKit.call(entity -> entity.delete());
    assertTrue(result.isReply());
    assertEquals(result.getReply(), "Deleted");
    assertEquals(testKit.getState(), 0);
  }
}
