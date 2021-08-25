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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.tck.model.eventing.LocalPersistenceSubscriberModel;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;

public class LocalPersistenceSubscriber extends Action {

  public LocalPersistenceSubscriber(ActionCreationContext creationContext) {}

  public Reply<LocalPersistenceEventing.Response> processEventOne(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.EventOne eventOne) {
    return convert(context, cloudEvent, eventOne.getStep());
  }

  public Source<Reply<LocalPersistenceEventing.Response>, NotUsed> processEventTwo(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.EventTwo eventTwo) {
    return Source.from(eventTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  public LocalPersistenceEventing.Response processAnyEvent(
      JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return LocalPersistenceEventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  public Reply<LocalPersistenceEventing.Response> processValueOne(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.ValueOne valueOne) {
    return convert(context, cloudEvent, valueOne.getStep());
  }

  public Source<Reply<LocalPersistenceEventing.Response>, NotUsed> processValueTwo(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.ValueTwo valueTwo) {
    return Source.from(valueTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  public LocalPersistenceEventing.Response processAnyValue(
      JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return LocalPersistenceEventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  public LocalPersistenceEventing.Response effect(LocalPersistenceEventing.EffectRequest request) {
    return LocalPersistenceEventing.Response.newBuilder()
        .setId(request.getId())
        .setMessage(request.getMessage())
        .build();
  }

  private Reply<LocalPersistenceEventing.Response> convert(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.ProcessStep step) {
    String id = cloudEvent.subject().orElse("");
    if (step.hasReply()) {
      return Reply.message(
          LocalPersistenceEventing.Response.newBuilder()
              .setId(id)
              .setMessage(step.getReply().getMessage())
              .build());
    } else if (step.hasForward()) {
      return Reply.forward(
          context
              .serviceCallFactory()
              .lookup(
                  LocalPersistenceSubscriberModel.name,
                  "Effect",
                  LocalPersistenceEventing.EffectRequest.class)
              .createCall(
                  LocalPersistenceEventing.EffectRequest.newBuilder()
                      .setId(id)
                      .setMessage(step.getForward().getMessage())
                      .build()));
    } else {
      throw new RuntimeException("No reply or forward");
    }
  }
}
