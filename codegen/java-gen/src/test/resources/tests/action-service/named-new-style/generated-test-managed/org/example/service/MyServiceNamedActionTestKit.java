package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.action.Action.Effect;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
import com.akkaserverless.javasdk.testkit.impl.TestKitActionContext;
import com.google.protobuf.Empty;
import org.example.service.MyServiceNamedAction;
import org.example.service.ServiceOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class MyServiceNamedActionTestKit {

  private Function<ActionCreationContext, MyServiceNamedAction> actionFactory;

  private MyServiceNamedAction createAction(TestKitActionContext context) {
    MyServiceNamedAction action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  public static MyServiceNamedActionTestKit of(Function<ActionCreationContext, MyServiceNamedAction> actionFactory) {
    return new MyServiceNamedActionTestKit(actionFactory);
  }

  private MyServiceNamedActionTestKit(Function<ActionCreationContext, MyServiceNamedAction> actionFactory) {
    this.actionFactory = actionFactory;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Effect<Empty> effect = createAction(context).simpleMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Source<Effect<Empty>, akka.NotUsed> effect = createAction(context).streamedOutputMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Effect<Empty> effect = createAction(context).streamedInputMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Source<Effect<Empty>, akka.NotUsed> effect = createAction(context).fullStreamedMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    return simpleMethod(myRequest, Metadata.EMPTY);
  }

  public Source<ActionResult<Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
    return streamedOutputMethod(myRequest, Metadata.EMPTY);
  }

  public ActionResult<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    return streamedInputMethod(myRequest, Metadata.EMPTY);
  }

  public Source<ActionResult<Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    return fullStreamedMethod(myRequest, Metadata.EMPTY);
  }

}
