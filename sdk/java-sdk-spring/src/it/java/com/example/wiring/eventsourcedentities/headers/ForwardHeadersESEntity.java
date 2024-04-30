/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.headers;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.ForwardHeaders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.example.wiring.actions.headers.ForwardHeadersAction.SOME_HEADER;

@Id("id")
@TypeId("forward-headers-es")
@RequestMapping("/forward-headers-es/{id}")
@ForwardHeaders(SOME_HEADER)
public class ForwardHeadersESEntity extends EventSourcedEntity<String, Object> {

  @PutMapping()
  public Effect<Message> createUser() {
    String headerValue = commandContext().metadata().get(SOME_HEADER).orElse("");
    return effects().reply(new Message(headerValue));
  }
}
