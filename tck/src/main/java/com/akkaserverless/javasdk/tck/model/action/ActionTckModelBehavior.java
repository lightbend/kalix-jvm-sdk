/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.tck.model.Action.*;
import com.akkaserverless.tck.model.ActionTwo;

import java.util.concurrent.CompletionStage;

public class ActionTckModelBehavior extends Action {

  private final ActorSystem system = ActorSystem.create("ActionTckModel");

  public ActionTckModelBehavior(ActionCreationContext creationContext) {}

  public CompletionStage<Reply<Response>> processUnary(Request request) {
    return Source.single(request).via(responses(actionContext())).runWith(singleResponse(), system);
  }

  public CompletionStage<Reply<Response>> processStreamedIn(Source<Request, NotUsed> requests) {
    return requests.via(responses(actionContext())).runWith(singleResponse(), system);
  }

  public Source<Reply<Response>, NotUsed> processStreamedOut(Request request) {
    return Source.single(request).via(responses(actionContext()));
  }

  public Source<Reply<Response>, NotUsed> processStreamed(Source<Request, NotUsed> requests) {
    return requests.via(responses(actionContext()));
  }

  private Flow<Request, Reply<Response>, NotUsed> responses(ActionContext context) {
    return Flow.of(Request.class)
        .flatMapConcat(request -> Source.from(request.getGroupsList()))
        .map(group -> response(group, context));
  }

  private Reply<Response> response(ProcessGroup group, ActionContext context) {
    Reply<Response> reply = Reply.noReply();
    for (ProcessStep step : group.getStepsList()) {
      switch (step.getStepCase()) {
        case REPLY:
          reply =
              Reply.message(Response.newBuilder().setMessage(step.getReply().getMessage()).build())
                  .addSideEffects(reply.sideEffects());
          break;
        case FORWARD:
          reply =
              Reply.<Response>forward(serviceTwoRequest(context, step.getForward().getId()))
                  .addSideEffects(reply.sideEffects());
          break;
        case EFFECT:
          SideEffect effect = step.getEffect();
          reply =
              reply.addSideEffects(
                  com.akkaserverless.javasdk.SideEffect.of(
                      serviceTwoRequest(context, effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          reply =
              Reply.<Response>failure(step.getFail().getMessage())
                  .addSideEffects(reply.sideEffects());
      }
    }
    return reply;
  }

  private Sink<Reply<Response>, CompletionStage<Reply<Response>>> singleResponse() {
    return Sink.fold(
        Reply.noReply(),
        (reply, next) ->
            next.isEmpty()
                ? reply.addSideEffects(next.sideEffects())
                : next.addSideEffects(reply.sideEffects()));
  }

  private ServiceCall serviceTwoRequest(Context context, String id) {
    return context
        .serviceCallFactory()
        .lookup(ActionTwo.name, "Call", OtherRequest.class)
        .createCall(OtherRequest.newBuilder().setId(id).build());
  }
}
