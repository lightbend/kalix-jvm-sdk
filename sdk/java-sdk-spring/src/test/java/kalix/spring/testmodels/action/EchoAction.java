/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.action;

import kalix.javasdk.action.Action;
import kalix.spring.testmodels.Message;
import org.springframework.web.bind.annotation.*;

public class EchoAction extends Action {

  @GetMapping("/echo/{msg}")
  public Effect<Message> stringMessage(@PathVariable String msg) {
    return effects().reply(new Message(msg));
  }

  @PostMapping("/echo")
  public Effect<Message> messageBody(@RequestParam("add") String add, @RequestBody Message msg) {
    return effects().reply(new Message(msg.value() + add));
  }
}
