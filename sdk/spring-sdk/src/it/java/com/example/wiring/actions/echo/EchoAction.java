/*
 * Copyright 2021 Lightbend Inc.
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class EchoAction extends Action {

  private Parrot parrot;
  private ActionCreationContext ctx;

  public EchoAction(Parrot parrot, ActionCreationContext ctx) {
    this.parrot = parrot;
    this.ctx = ctx;
  }

  @GetMapping("/echo/message/{msg}")
  public Effect<Message> stringMessage(@PathVariable String msg) {
    String response = this.parrot.repeat(msg);
    return effects().reply(new Message(response));
  }

  @GetMapping("/echo/message")
  public Effect<Message> stringMessageFromParam(@RequestParam String msg) {
    return stringMessage(msg);
  }

  @GetMapping("/echo/repeat/{msg}/times/{times}")
  public Flux<Effect<Message>> stringMessage(
      @PathVariable String msg, @PathVariable Integer times) {
    return Flux.range(1, times)
        // add an async boundary just to have some thread switching
        .flatMap(
            i -> Mono.fromCompletionStage(CompletableFuture.supplyAsync(() -> parrot.repeat(msg))))
        .map(m -> effects().reply(new Message(m)));
  }

  @GetMapping("/async-echo/message/{msg}")
  public Effect<Message> stringMessageAsyncReply(@PathVariable String msg) {
    String response = this.parrot.repeat(msg);
    return effects()
        .asyncReply(Mono.fromCompletionStage(
            CompletableFuture.completedFuture(new Message(response))));
  }

  @GetMapping("/async-echo-flux/message/{msg}")
  public Effect<Message> stringMessageAsyncReplyFlux(@PathVariable String msg) {
    String response = this.parrot.repeat(msg);
    return effects()
        .asyncReply(Flux.fromStream(Stream.of(new Message(response), new Message("ignored"))));
  }
}
