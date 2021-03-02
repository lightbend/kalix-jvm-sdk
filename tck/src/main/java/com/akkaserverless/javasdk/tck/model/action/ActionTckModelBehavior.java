/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.action;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.action.*;
import com.akkaserverless.tck.model.Action.*;
import com.akkaserverless.tck.model.ActionTwo;

import java.util.concurrent.CompletionStage;

@Action
public class ActionTckModelBehavior {

  private final ActorSystem system = ActorSystem.create("ActionTckModel");

  public ActionTckModelBehavior() {}

  @CommandHandler
  public CompletionStage<ActionReply<Response>> processUnary(
      Request request, ActionContext context) {
    return Source.single(request).via(responses(context)).runWith(singleResponse(), system);
  }

  @CommandHandler
  public CompletionStage<ActionReply<Response>> processStreamedIn(
      Source<Request, NotUsed> requests, ActionContext context) {
    return requests.via(responses(context)).runWith(singleResponse(), system);
  }

  @CommandHandler
  public Source<ActionReply<Response>, NotUsed> processStreamedOut(
      Request request, ActionContext context) {
    return Source.single(request).via(responses(context));
  }

  @CommandHandler
  public Source<ActionReply<Response>, NotUsed> processStreamed(
      Source<Request, NotUsed> requests, ActionContext context) {
    return requests.via(responses(context));
  }

  private Flow<Request, ActionReply<Response>, NotUsed> responses(ActionContext context) {
    return Flow.of(Request.class)
        .flatMapConcat(request -> Source.from(request.getGroupsList()))
        .map(group -> response(group, context));
  }

  private ActionReply<Response> response(ProcessGroup group, ActionContext context) {
    ActionReply<Response> reply = ActionReply.noReply();
    for (ProcessStep step : group.getStepsList()) {
      switch (step.getStepCase()) {
        case REPLY:
          reply =
              ActionReply.message(
                      Response.newBuilder().setMessage(step.getReply().getMessage()).build())
                  .withEffects(reply.effects());
          break;
        case FORWARD:
          reply =
              ActionReply.<Response>forward(serviceTwoRequest(context, step.getForward().getId()))
                  .withEffects(reply.effects());
          break;
        case EFFECT:
          SideEffect effect = step.getEffect();
          reply =
              reply.withEffects(
                  Effect.of(serviceTwoRequest(context, effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          reply =
              ActionReply.<Response>failure(step.getFail().getMessage())
                  .withEffects(reply.effects());
      }
    }
    return reply;
  }

  private Sink<ActionReply<Response>, CompletionStage<ActionReply<Response>>> singleResponse() {
    return Sink.fold(
        ActionReply.noReply(),
        (reply, next) ->
            next.isEmpty() ? reply.withEffects(next.effects()) : next.withEffects(reply.effects()));
  }

  private ServiceCall serviceTwoRequest(Context context, String id) {
    return context
        .serviceCallFactory()
        .lookup(ActionTwo.name, "Call", OtherRequest.class)
        .createCall(OtherRequest.newBuilder().setId(id).build());
  }
}
