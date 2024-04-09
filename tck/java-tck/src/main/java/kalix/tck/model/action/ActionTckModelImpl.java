/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.action;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.javasdk.action.*;
import kalix.tck.model.action.Action.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class ActionTckModelImpl extends AbstractActionTckModelAction {

  public ActionTckModelImpl(ActionCreationContext creationContext) {}

  public Effect<Response> processUnary(Request request) {
    // Multiple request steps should be combined to give just one response, with subsequent steps
    // taking precedence.
    return response(request.getGroupsList());
  }

  public Effect<Response> processStreamedIn(Source<Request, NotUsed> requests) {
    // All request steps should be combined to produce a single response after the request stream
    // completes.
    CompletionStage<ArrayList<ProcessGroup>> processGroups =
        requests
            .fold(
                new ArrayList<ProcessGroup>(),
                (acc, request) -> {
                  acc.addAll(request.getGroupsList());
                  return acc;
                })
            .runWith(Sink.head(), actionContext().materializer());
    CompletionStage<Effect<Response>> effect = processGroups.thenApply(groups -> response(groups));
    return effects().asyncEffect(effect);
  }

  public Source<Effect<Response>, NotUsed> processStreamedOut(Request request) {
    // The single request may contain multiple grouped steps, each group corresponding to an
    // expected response.
    return Source.from(request.getGroupsList())
        .map(groupList -> response(Collections.singletonList(groupList)));
  }

  public Source<Effect<Response>, NotUsed> processStreamed(Source<Request, NotUsed> requests) {
    // Each request may contain multiple grouped steps, each group corresponding to an expected
    // response.
    return requests
        .mapConcat(request -> request.getGroupsList())
        .map(groupList -> response(Collections.singletonList(groupList)));
  }

  private Effect<Response> response(List<ProcessGroup> groups) {
    Effect<Response> effect = null;
    List<SideEffect> sideEffects = new ArrayList<>();
    // TCK tests expect the logic to be imperative, building something up and then discarding on
    // failure but effect
    // api is not imperative so we have to keep track of failure instead
    boolean didFail = false;
    for (ProcessGroup group : groups) {
      for (ProcessStep step : group.getStepsList()) {
        switch (step.getStepCase()) {
          case REPLY:
            if (!didFail) {
              effect =
                  effects()
                      .reply(
                          Response.newBuilder().setMessage(step.getReply().getMessage()).build());
            }
            break;
          case FORWARD:
            if (!didFail) {
              effect = effects().forward(serviceTwoRequest(step.getForward().getId()));
            }
            break;
          case EFFECT:
            Action.SideEffect sideEffect = step.getEffect();
            sideEffects.add(
                kalix.javasdk.SideEffect.of(
                    serviceTwoRequest(sideEffect.getId()), sideEffect.getSynchronous()));
            break;
          case FAIL:
            effect = effects().error(step.getFail().getMessage());
            didFail = true;
        }
      }
    }
    if (effect == null) {
      effect = effects().reply(Response.getDefaultInstance());
    }
    return effect.addSideEffects(sideEffects);
  }

  private DeferredCall serviceTwoRequest(String id) {
    return components().actionTwoImpl().call(OtherRequest.newBuilder().setId(id).build());
  }
}
