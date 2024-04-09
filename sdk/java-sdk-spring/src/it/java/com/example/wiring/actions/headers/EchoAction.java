/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.headers;

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Action with the same name in a different package.
 */
public class EchoAction extends Action {

  @GetMapping("/echo2/message/{msg}")
  public Effect<Message> stringMessage(@PathVariable String msg) {
    return effects().reply(new Message(msg));
  }
}
