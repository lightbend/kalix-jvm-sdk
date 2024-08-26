package com.example.action;

import com.example.CounterEntity;
import kalix.javasdk.SideEffect;
import kalix.javasdk.action.Action;
import com.example.Number;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


public class DoubleCounterAction extends Action {

  final private ComponentClient componentClient;

  public DoubleCounterAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  // tag::controller-side-effect[]
  @PostMapping("/counter/{counterId}/double-increase/{value}") // <1>
  public Action.Effect<String> increaseWithSideEffect(@PathVariable String counterId, @PathVariable int value) {
    var doubleIncrease = value * 2; // <2>
    var deferredCall = componentClient.forValueEntity(counterId)
      .call(CounterEntity::increaseBy)
      .params(new Number(doubleIncrease));
    return effects().reply("ok").addSideEffect(SideEffect.of(deferredCall));  // <3>
  }
  // end::controller-side-effect[]
}
