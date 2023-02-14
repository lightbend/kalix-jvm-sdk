package com.example.callanotherservice;

import kalix.javasdk.action.Action;
import kalix.spring.WebClientProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;

// tag::delegating-action[]
public class DelegatingServiceAction extends Action {

  final private WebClient webClient;

  public DelegatingServiceAction(WebClientProvider webClientProvider) { // <1>
    this.webClient = webClientProvider.webClientFor("counter"); // <2>
  }

  @PostMapping("/delegate/counter/{counter_id}/increase")
  public Effect<Number> addAndReturn(@PathVariable String counterId, @RequestBody Number increaseBy) {
    var result =
        webClient
            .post().uri("/counter/" + counterId + "/increase") // <3>
            .bodyValue(increaseBy)
            .retrieve()
            .bodyToMono(Number.class).toFuture();

    return effects().asyncReply(result);  // <4>
  }
}
// end::delegating-action[]
