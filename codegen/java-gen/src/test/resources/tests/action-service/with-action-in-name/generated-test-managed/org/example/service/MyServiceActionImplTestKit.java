package org.example.service;

import kalix.javasdk.action.Action.Effect;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.impl.action.ActionEffectImpl;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.impl.ActionResultImpl;
import kalix.javasdk.testkit.impl.TestKitActionContext;
import com.google.protobuf.Empty;
import org.example.service.MyServiceActionImpl;
import org.example.service.ServiceOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class MyServiceActionImplTestKit {

  private Function<ActionCreationContext, MyServiceActionImpl> actionFactory;

  private MyServiceActionImpl createAction() {
    MyServiceActionImpl action = actionFactory.apply(new TestKitActionContext());
    action._internalSetActionContext(Optional.of(new TestKitActionContext()));
    return action;
  };

  public static MyServiceActionImplTestKit of(Function<ActionCreationContext, MyServiceActionImpl> actionFactory) {
    return new MyServiceActionImplTestKit(actionFactory);
  }

  private MyServiceActionImplTestKit(Function<ActionCreationContext, MyServiceActionImpl> actionFactory) {
    this.actionFactory = actionFactory;
  }

  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
    return new ActionResultImpl(effect);
  }

  public ActionResult<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    Effect<Empty> effect = createAction().simpleMethod(myRequest);
    return interpretEffects(effect);
  }

}
