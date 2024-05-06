package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomNameAction extends Action {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetRandomNameAction.class);

  private final ComponentClient componentClient;

  private final WebClient webClient;

  public GetRandomNameAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.webClient = WebClient.create("https://randomuser.me/api/?inc=name&noinfo");
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    if (actionContext().eventSubject().isPresent()) {
      var randomNameFut = getRandomNameAsync();

      var updateNameCall = randomNameFut.thenCompose(name ->
          componentClient
            .forEventSourcedEntity(actionContext().eventSubject().get())
            .call(UserEntity::updateName)
            .params(new UserEntity.UserCmd.UpdateNameCmd(name))
            .execute());

      return effects().asyncReply(updateNameCall);
    } else {
      return effects().ignore();
    }
  }

  // gets random name from external API using a synchronous call and traces that call
  private CompletableFuture<String> getRandomNameAsync() {
    var otelCurrentContext = actionContext().metadata().traceContext().asOpenTelemetryContext();
    Span span = actionContext().getTracer()
        .spanBuilder("random-name-async")
        .setParent(otelCurrentContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      CompletableFuture<RandomUserApi.Name> result = webClient.get()
          .retrieve()
          .bodyToMono(RandomUserApi.Name.class)
          .toFuture();

      // set attrs on span
      result.thenAccept(name -> {
        span.setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));
        span.setAttribute("random.name", name.name());
        span.end();
      });

      return result.thenApply(RandomUserApi.Name::name);
    }
  }
}
