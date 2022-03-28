package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.action.Action.Effect;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.impl.action.ActionEffectImpl;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;
import org.example.service.MyServiceAction;
import org.example.service.ServiceOuterClass;
import org.external.ExternalDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class MyServiceActionTestKit {

  private Function<ActionCreationContext, MyServiceAction> actionFactory;

  private MyServiceAction createAction() {
    MyServiceAction action = actionFactory.apply(new TestKitActionContext());
    action._internalSetActionContext(Optional.of(new TestKitActionContext()));
    return action;
  };

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    return new MyServiceActionTestKit(actionFactory);
  }

  private MyServiceActionTestKit(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    this.actionFactory = actionFactory;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    Effect<ExternalDomain.Empty> effect = createAction().simpleMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
    Source<Effect<ExternalDomain.Empty>, akka.NotUsed> effect = createAction().streamedOutputMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    Effect<ExternalDomain.Empty> effect = createAction().streamedInputMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    Source<Effect<ExternalDomain.Empty>, akka.NotUsed> effect = createAction().fullStreamedMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

}
