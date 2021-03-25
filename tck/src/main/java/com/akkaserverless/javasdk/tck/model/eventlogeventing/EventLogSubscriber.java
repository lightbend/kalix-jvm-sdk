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
import com.akkaserverless.javasdk.action.Handler;
import com.akkaserverless.tck.model.EventLogSubscriberModel;
import com.akkaserverless.tck.model.EventLogEventing;

@Action
public class EventLogSubscriber {

  @Handler
  public ActionReply<EventLogEventing.Response> processEventOne(
      ActionContext context, CloudEvent cloudEvent, EventLogEventing.EventOne eventOne) {
    return convert(context, cloudEvent, eventOne.getStep());
  }

  @Handler
  public Source<ActionReply<EventLogEventing.Response>, NotUsed> processEventTwo(
      ActionContext context, CloudEvent cloudEvent, EventLogEventing.EventTwo eventTwo) {
    return Source.from(eventTwo.getStepList()).map(step -> convert(context, cloudEvent, step));
  }

  @Handler
  public EventLogEventing.Response effect(EventLogEventing.EffectRequest request) {
    return EventLogEventing.Response.newBuilder()
        .setId(request.getId())
        .setMessage(request.getMessage())
        .build();
  }

  @Handler
  public EventLogEventing.Response processAnyEvent(JsonMessage jsonMessage, CloudEvent cloudEvent) {
    return EventLogEventing.Response.newBuilder()
        .setId(cloudEvent.subject().orElse(""))
        .setMessage(jsonMessage.message)
        .build();
  }

  private ActionReply<EventLogEventing.Response> convert(
      ActionContext context, CloudEvent cloudEvent, EventLogEventing.ProcessStep step) {
    String id = cloudEvent.subject().orElse("");
    if (step.hasReply()) {
      return ActionReply.message(
          EventLogEventing.Response.newBuilder()
              .setId(id)
              .setMessage(step.getReply().getMessage())
              .build());
    } else if (step.hasForward()) {
      return ActionReply.forward(
          context
              .serviceCallFactory()
              .lookup(EventLogSubscriberModel.name, "Effect", EventLogEventing.EffectRequest.class)
              .createCall(
                  EventLogEventing.EffectRequest.newBuilder()
                      .setId(id)
                      .setMessage(step.getForward().getMessage())
                      .build()));
    } else {
      throw new RuntimeException("No reply or forward");
    }
  }
}
