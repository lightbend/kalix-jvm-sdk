/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.echo;

import kalix.javasdk.HttpResponse;
import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import org.springframework.web.bind.annotation.GetMapping;

public class ActionWithHttpResponse extends Action {

  @GetMapping("/text-body")
  public Effect<HttpResponse> textBody() {
    return effects().reply(HttpResponse.ok("test"));
  }

  @GetMapping("/empty-text-body")
  public Effect<HttpResponse> emptyCreated() {
    return effects().reply(HttpResponse.created());
  }

  @GetMapping("/json-string-body")
  public Effect<HttpResponse> jsonStringBody() {
    return effects().reply(HttpResponse.of(StatusCode.Success.OK, "application/json", "{\"text\": \"123\"}".getBytes()));
  }

  @GetMapping("/json-body")
  public Effect<HttpResponse> jsonBody() {
    return effects().reply(HttpResponse.ok(new Message("321")));
  }

}
