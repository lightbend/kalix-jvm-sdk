/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.action.Handler;
import com.akkaserverless.tck.model.eventing.LocalPersistenceSubscriberModel;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;

@Action
public class LocalPersistenceSubscriber {

  @Handler
  public Reply<LocalPersistenceEventing.Response> processEventOne(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.EventOne eventOne) {
    return convert(context, cloudEvent, eventOne.getStep());
  }

  @Handler
  public Source<Reply<LocalPersistenceEventing.Response>, NotUsed> processEventTwo(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.EventTwo eventTwo) {
    return Source.from(eventTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  @Handler
  public LocalPersistenceEventing.Response processAnyEvent(
      JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return LocalPersistenceEventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  @Handler
  public Reply<LocalPersistenceEventing.Response> processValueOne(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.ValueOne valueOne) {
    return convert(context, cloudEvent, valueOne.getStep());
  }

  @Handler
  public Source<Reply<LocalPersistenceEventing.Response>, NotUsed> processValueTwo(
      ActionContext context, CloudEvent cloudEvent, LocalPersistenceEventing.ValueTwo valueTwo) {
    return Source.from(valueTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  @Handler
  public LocalPersistenceEventing.Response processAnyValue(
      JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return LocalPersistenceEventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  @Handler
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
