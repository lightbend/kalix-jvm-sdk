package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.Action.Effect;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
import com.akkaserverless.javasdk.testkit.impl.TestKitActionContext;
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

  private TestKitActionContext context;

  private MyServiceAction createAction() {
    MyServiceAction action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  };

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    return new MyServiceActionTestKit(actionFactory, new TestKitActionContext());
  }

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory, TestKitActionContext context) {
    return new MyServiceActionTestKit(actionFactory, context);
  }

  private MyServiceActionTestKit(Function<ActionCreationContext, MyServiceAction> actionFactory, TestKitActionContext context) {
    this.actionFactory = actionFactory;
    this.context = context;
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
