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

package kalix.spring.testmodels.valueentity;

import kalix.javasdk.annotations.GenerateId;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.spring.testmodels.Number;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("counterId")
@TypeId("ve-counter")
@RequestMapping("/counter")
public class Counter extends ValueEntity<CounterState> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public CounterState emptyState() {
    return new CounterState(commandContext().entityId(), 0);
  }

  @PostMapping("/{counterId}/increase")
  public ValueEntity.Effect<Number> increase(@RequestBody Number num) {
    CounterState counterState = currentState();
    logger.info(
        "Increasing counter '{}' by '{}', current value is '{}'",
        counterState.id,
        num.value,
        counterState.value);
    CounterState newCounter = counterState.increase(num.value);
    return effects().updateState(newCounter).thenReply(new Number(newCounter.value));
  }

  @GenerateId
  @PostMapping("/increase/{value}")
  public ValueEntity.Effect<Number> randomIncrease(@PathVariable Integer value) {
    CounterState counterState = new CounterState(commandContext().entityId(), value);
    logger.info(
        "Increasing counter '{}' to value '{}'",
        counterState.id,
        counterState.value);
    return effects().updateState(counterState).thenReply(new Number(counterState.value));
  }

  @GetMapping("/{counterId}")
  public ValueEntity.Effect<Number> get() {
    logger.info("Counter '{}' is '{}'", commandContext().entityId(), currentState().value);
    return effects().reply(new Number(currentState().value));
  }
}
