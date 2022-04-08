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

  private MyServiceAction createAction(TestKitActionContext context) {
    MyServiceAction action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    return new MyServiceActionTestKit(actionFactory);
  }

  private MyServiceActionTestKit(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    this.actionFactory = actionFactory;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Effect<ExternalDomain.Empty> effect = createAction(context).simpleMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Source<Effect<ExternalDomain.Empty>, akka.NotUsed> effect = createAction(context).streamedOutputMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Effect<ExternalDomain.Empty> effect = createAction(context).streamedInputMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata);
    Source<Effect<ExternalDomain.Empty>, akka.NotUsed> effect = createAction(context).fullStreamedMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    return simpleMethod(myRequest, Metadata.EMPTY);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
    return streamedOutputMethod(myRequest, Metadata.EMPTY);
  }

  public ActionResult<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    return streamedInputMethod(myRequest, Metadata.EMPTY);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
    return fullStreamedMethod(myRequest, Metadata.EMPTY);
  }

}
