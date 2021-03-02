/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.eventlogeventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.ActionReply;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.akkaserverless.tck.model.EventLogSubscriberModel;
import com.akkaserverless.tck.model.Eventlogeventing;

@Action
public class EventLogSubscriber {

  @CommandHandler
  public ActionReply<Eventlogeventing.Response> processEventOne(
      ActionContext context, CloudEvent cloudEvent, Eventlogeventing.EventOne eventOne) {
    return convert(context, cloudEvent, eventOne.getStep());
  }

  @CommandHandler
  public Source<ActionReply<Eventlogeventing.Response>, NotUsed> processEventTwo(
      ActionContext context, CloudEvent cloudEvent, Eventlogeventing.EventTwo eventTwo) {
    return Source.from(eventTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  @CommandHandler
  public Eventlogeventing.Response effect(Eventlogeventing.EffectRequest request) {
    return Eventlogeventing.Response.newBuilder()
        .setId(request.getId())
        .setMessage(request.getMessage())
        .build();
  }

  @CommandHandler
  public Eventlogeventing.Response processAnyEvent(JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return Eventlogeventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  private ActionReply<Eventlogeventing.Response> convert(
      ActionContext context, CloudEvent cloudEvent, Eventlogeventing.ProcessStep step) {
    String id = cloudEvent.subject().orElse("");
    if (step.hasReply()) {
      return ActionReply.message(
          Eventlogeventing.Response.newBuilder()
              .setId(id)
              .setMessage(step.getReply().getMessage())
              .build());
    } else if (step.hasForward()) {
      return ActionReply.forward(
          context
              .serviceCallFactory()
              .lookup(EventLogSubscriberModel.name, "Effect", Eventlogeventing.EffectRequest.class)
              .createCall(
                  Eventlogeventing.EffectRequest.newBuilder()
                      .setId(id)
                      .setMessage(step.getForward().getMessage())
                      .build()));
    } else {
      throw new RuntimeException("No reply or forward");
    }
  }
}
