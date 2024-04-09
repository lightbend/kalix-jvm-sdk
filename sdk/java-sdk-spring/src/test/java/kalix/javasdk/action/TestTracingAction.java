/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.action;


import org.springframework.web.bind.annotation.GetMapping;

public class TestTracingAction extends Action {

  @GetMapping("/tracing/traceparent")
  public Effect<String> endpoint() {
    return effects().reply(
        actionContext().metadata().traceContext().traceParent().orElse("not-found"));
  }
}
