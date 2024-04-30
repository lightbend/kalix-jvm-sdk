/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.headers;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.ForwardHeaders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.example.wiring.actions.headers.ForwardHeadersAction.SOME_HEADER;

@Id("id")
@TypeId("forward-headers-ve")
@RequestMapping("/forward-headers-ve/{id}")
@ForwardHeaders(SOME_HEADER)
public class ForwardHeadersValueEntity extends ValueEntity<String> {

  @PutMapping()
  public Effect<Message> createUser() {
    String headerValue = commandContext().metadata().get(SOME_HEADER).orElse("");
    return effects().reply(new Message(headerValue));
  }
}
