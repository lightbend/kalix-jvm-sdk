/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("ve")
@RequestMapping("/ve")
public class TestValueEntity extends ValueEntity<TestVEState1> {

  @Override
  public TestVEState1 emptyState() {
    return new TestVEState1("empty", 1);
  }

  @GetMapping
  public Effect<TestVEState1> get() {
    return effects().reply(currentState());
  }

}
