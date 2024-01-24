package org.example.service;

import com.google.protobuf.Empty;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action.Effect;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.MockRegistry;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;
import org.example.service.SomeServiceAction;
import org.example.service.SomeServiceOuterClass;

import java.util.Optional;
import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class SomeServiceActionTestKit {

  private final Function<ActionCreationContext, SomeServiceAction> actionFactory;

  private final MockRegistry mockRegistry;

  private SomeServiceAction createAction(TestKitActionContext context) {
    SomeServiceAction action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  public static SomeServiceActionTestKit of(Function<ActionCreationContext, SomeServiceAction> actionFactory) {
    return new SomeServiceActionTestKit(actionFactory, MockRegistry.EMPTY);
  }

  public static SomeServiceActionTestKit of(Function<ActionCreationContext, SomeServiceAction> actionFactory, MockRegistry mockRegistry) {
    return new SomeServiceActionTestKit(actionFactory, mockRegistry);
  }

  private SomeServiceActionTestKit(Function<ActionCreationContext, SomeServiceAction> actionFactory, MockRegistry mockRegistry) {
    this.actionFactory = actionFactory;
    this.mockRegistry = mockRegistry;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<Empty> simpleMethod(SomeServiceOuterClass.SomeRequest someRequest, Metadata metadata) {
    TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
    Effect<Empty> effect = createAction(context).simpleMethod(someRequest);
    return interpretEffects(effect);
  }

  public ActionResult<Empty> simpleMethod(SomeServiceOuterClass.SomeRequest someRequest) {
    return simpleMethod(someRequest, Metadata.EMPTY);
  }

}
