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
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.action.*;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.tck.model.Action.*;
import com.akkaserverless.tck.model.ActionTwo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class ActionTckModelBehavior extends Action {

  private final ActorSystem system = ActorSystem.create("ActionTckModel");

  public ActionTckModelBehavior(ActionCreationContext creationContext) {}

  public Effect<Response> processUnary(Request request) {
    return effects()
        .flatten(Source.single(request).via(responses()).runWith(singleResponse(), system));
  }

  public Effect<Response> processStreamedIn(Source<Request, NotUsed> requests) {
    return effects().flatten(requests.via(responses()).runWith(singleResponse(), system));
  }

  public Source<Effect<Response>, NotUsed> processStreamedOut(Request request) {
    return Source.single(request).via(responses());
  }

  public Source<Effect<Response>, NotUsed> processStreamed(Source<Request, NotUsed> requests) {
    return requests.via(responses());
  }

  private Flow<Request, Effect<Response>, NotUsed> responses() {
    return Flow.of(Request.class)
        .flatMapConcat(request -> Source.from(request.getGroupsList()))
        .map(group -> response(group));
  }

  private Effect<Response> response(ProcessGroup group) {
    Effect<Response> effect = effects().noReply();
    List<SideEffect> sideEffects = new ArrayList<>();
    for (ProcessStep step : group.getStepsList()) {
      switch (step.getStepCase()) {
        case REPLY:
          effect =
              effects()
                  .message(Response.newBuilder().setMessage(step.getReply().getMessage()).build());
          break;
        case FORWARD:
          effect = effects().forward(serviceTwoRequest(step.getForward().getId()));
          break;
        case EFFECT:
          com.akkaserverless.tck.model.Action.SideEffect sideEffect = step.getEffect();
          sideEffects.add(
              com.akkaserverless.javasdk.SideEffect.of(
                  serviceTwoRequest(sideEffect.getId()), sideEffect.getSynchronous()));
          break;
        case FAIL:
          effect = effects().error(step.getFail().getMessage());
      }
    }
    return effect.addSideEffects(sideEffects);
  }

  private Sink<Effect<Response>, CompletionStage<Effect<Response>>> singleResponse() {
    return Sink.fold(
        effects().noReply(),
        (reply, next) ->
            next.isEmpty()
                ? reply.addSideEffects(next.sideEffects())
                : next.addSideEffects(reply.sideEffects()));
  }

  private ServiceCall serviceTwoRequest(String id) {
    return actionContext()
        .serviceCallFactory()
        .lookup(ActionTwo.name, "Call", OtherRequest.class)
        .createCall(OtherRequest.newBuilder().setId(id).build());
  }
}
