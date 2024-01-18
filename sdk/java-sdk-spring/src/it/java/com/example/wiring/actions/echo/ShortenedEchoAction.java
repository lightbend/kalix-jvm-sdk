/*
 * Copyright 2024 Lightbend Inc.
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

package com.example.wiring.actions.echo;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.*;

public class ShortenedEchoAction extends Action {

  private ActionCreationContext ctx;
  private ComponentClient componentClient;

  public ShortenedEchoAction(ActionCreationContext ctx, ComponentClient componentClient) {
    this.ctx = ctx;
    this.componentClient = componentClient;
  }

  @GetMapping("/echo/message/{msg}/short")
  public Effect<Message> stringMessage(@PathVariable String msg) {
    var shortenedMsg = msg.replaceAll("[AEIOUaeiou]", "");
    var result = componentClient.forAction().call(EchoAction::stringMessage).params(shortenedMsg).execute();
    return effects().asyncReply(result);
  }

  @GetMapping("/echo/message-short")
  public Effect<Message> leetShortUsingFwd(@RequestParam String msg) {
    var shortenedMsg = leetShort(msg);
    var result = componentClient.forAction().call(EchoAction::stringMessageFromParam).params(shortenedMsg);
    return effects().forward(result);
  }

  @GetMapping("/echo/message/{msg}/leetshort")
  public Effect<Message> leetMessageFromPathUsingFwd(@PathVariable String msg) {
    return leetShortUsingFwd(msg);
  }

  @PostMapping("/echo/message/leetshort")
  public Effect<Message> leetMessageWithFwdPost(@RequestBody Message msg) {
    var shortenedMsg = leetShort(msg.text());
    var result = componentClient.forAction().call(EchoAction::stringMessage).params(shortenedMsg);
    return effects().forward(result);
  }

  private String leetShort(String msg) {
    return msg
            .replaceAll("[Ee]", "3")
            .replaceAll("[Aa]", "4")
            .replaceAll("[AEIOUaeiou]", "");
  }
}
