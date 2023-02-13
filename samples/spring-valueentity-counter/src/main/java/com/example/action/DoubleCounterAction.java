package com.example.action;

import com.example.CounterEntity;
import kalix.javasdk.SideEffect;
import kalix.javasdk.action.Action;
import com.example.Number;
import kalix.spring.KalixClient;
import kalix.javasdk.annotations.Subscribe;


@Subscribe.ValueEntity(CounterEntity.class)
public class DoubleCounterAction extends Action {

    final private KalixClient kalixClient;

    public DoubleCounterAction(KalixClient kalixClient){
        this.kalixClient = kalixClient;
    }

   // tag::controller-side-effect[]
    public Action.Effect<Confirmed> increaseWithSideEffect(Number increase){
        var counterId = actionContext().eventSubject().get(); // <1>
        var doubleIncrease = increase.value() * 2; // <2>
        var deferredCall = kalixClient.post("/counter/" + counterId + "/increase/" + doubleIncrease, Number.class);
        return effects().reply(Confirmed.instance).addSideEffect(SideEffect.of(deferredCall));  // <3>
    }
    // end::controller-side-effect[]
}
