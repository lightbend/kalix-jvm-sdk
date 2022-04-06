package org.example.service;

import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.action.Action.Effect;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
import com.akkaserverless.javasdk.testkit.impl.TestKitActionContext;
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

  private MyServiceActionImpl createAction(TestKitActionContext context) {
    MyServiceActionImpl action = actionFactory.apply(context);
    action._internalSetActionContext(Optional.of(context));
    return action;
  }

  public static MyServiceActionImplTestKit of(Function<ActionCreationContext, MyServiceActionImpl> actionFactory) {
    return new MyServiceActionImplTestKit(actionFactory);
  }

  private MyServiceActionImplTestKit(Function<ActionCreationContext, MyServiceActionImpl> actionFactory) {
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

  public ActionResult<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    return simpleMethod(myRequest, Metadata.EMPTY);
  }

}
