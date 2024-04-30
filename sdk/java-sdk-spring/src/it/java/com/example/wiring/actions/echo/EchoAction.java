/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.echo;

import kalix.javasdk.Metadata;
import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.impl.KalixClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EchoAction extends Action {

  private Parrot parrot;
  private ActionCreationContext ctx;
  private final ComponentClient componentClient;

  public EchoAction(Parrot parrot, ActionCreationContext ctx,  ComponentClient componentClient) {
    this.parrot = parrot;
    this.ctx = ctx;
    this.componentClient = componentClient;
  }

  @GetMapping("/echo/message/{msg_value}")
  public Effect<Message> stringMessage(@PathVariable("msg_value") String msg) {
    String response = this.parrot.repeat(msg);
    return effects().reply(new Message(response));
  }

  @GetMapping("/echo/int/{int_value}")
  public Effect<Message> intMessage(@PathVariable("int_value") Integer i) {
    String response = this.parrot.repeat(i+"");
    return effects().reply(new Message(response));
  }


  @GetMapping("/echo/message")
  public Effect<Message> stringMessageFromParam(@RequestParam String msg) {
    return stringMessage(msg);
  }

  @PostMapping("/echo/message/forward")
  public Effect<Message> stringMessageFromParamFwTyped(@RequestParam String msg) {
    var result = componentClient.forAction().call(EchoAction::stringMessageFromParam).params(msg);
    return effects().forward(result);
  }

  @PostMapping("/echo/message/concat")
  public Effect<Message> stringMessageConcatRequestBody(@RequestBody List<Message> messages) {
    var allMessages = messages.stream().map(m -> m.text()).collect(Collectors.joining("|"));
    return effects().reply(new Message(allMessages));
  }

  @PostMapping("/echo/message/concat/{separator}")
  public Effect<Message> stringMessageConcatRequestBodyWithSeparator(@PathVariable String separator, @RequestBody List<Message> messages ) {
    var allMessages = messages.stream().map(m -> m.text()).collect(Collectors.joining(separator));
    return effects().reply(new Message(allMessages));
  }

  @GetMapping("/echo/repeat/{msg}/times/{times}")
  public Flux<Effect<Message>> stringMessageRepeat(
      @PathVariable String msg, @PathVariable Integer times) {
    return Flux.range(1, times)
        // add an async boundary just to have some thread switching
        .flatMap(
            i -> Mono.fromCompletionStage(CompletableFuture.supplyAsync(() -> parrot.repeat(msg))))
        .map(m -> effects().reply(new Message(m)));
  }

  @PostMapping("/echo/message/customCode/{msg}")
  public Effect<Message> stringMessageCustomCode(@PathVariable String msg) {
    String response = this.parrot.repeat(msg);
    return effects().reply(new Message(response),
        Metadata.EMPTY.withStatusCode(StatusCode.Success.ACCEPTED));
  }
}
