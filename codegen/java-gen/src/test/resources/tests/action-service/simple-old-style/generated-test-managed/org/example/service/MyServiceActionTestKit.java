package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action.Effect;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.MockRegistry;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;
import org.example.service.MyServiceAction;
import org.example.service.ServiceOuterClass;
import org.external.ExternalDomain;

import java.util.Optional;
import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class MyServiceActionTestKit {

  private final Function<ActionCreationContext, MyServiceAction> actionFactory;

  private final MockRegistry mockRegistry;

  private MyServiceAction createAction(TestKitActionContext context) {
    MyServiceAction action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    return new MyServiceActionTestKit(actionFactory, MockRegistry.EMPTY);
  }

  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory, MockRegistry mockRegistry) {
    return new MyServiceActionTestKit(actionFactory, mockRegistry);
  }

  private MyServiceActionTestKit(Function<ActionCreationContext, MyServiceAction> actionFactory, MockRegistry mockRegistry) {
    this.actionFactory = actionFactory;
    this.mockRegistry = mockRegistry;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
    Effect<ExternalDomain.Empty> effect = createAction(context).simpleMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
    Source<Effect<ExternalDomain.Empty>, akka.NotUsed> effect = createAction(context).streamedOutputMethod(myRequest);
    return effect.map(e -> interpretEffects(e));
  }

  public ActionResult<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
    Effect<ExternalDomain.Empty> effect = createAction(context).streamedInputMethod(myRequest);
    return interpretEffects(effect);
  }

  public Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
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
