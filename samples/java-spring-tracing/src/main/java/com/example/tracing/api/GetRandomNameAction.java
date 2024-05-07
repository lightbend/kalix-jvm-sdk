package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.*;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomNameAction extends Action {

  private static final Logger log = LoggerFactory.getLogger(GetRandomNameAction.class);

  private final ComponentClient componentClient;

  private final WebClient webClient;

  public GetRandomNameAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.webClient = WebClient.create("https://randomuser.me/api/?inc=name&noinfo");
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    if (actionContext().eventSubject().isPresent()) {
      var randomNameFut = getRandomNameAsync().thenCompose(name ->
          componentClient
            .forEventSourcedEntity(actionContext().eventSubject().get())
            .call(UserEntity::updateName)
            .params(new UserEntity.UserCmd.UpdateNameCmd(name))
            .execute());

      return effects().asyncReply(randomNameFut);
    } else {
      return effects().ignore();
    }
  }

  // gets random name from external API using an asynchronous call and traces that call
  private CompletableFuture<String> getRandomNameAsync() {
    var otelCurrentContext = actionContext().metadata().traceContext().asOpenTelemetryContext();
    Span span = actionContext().getTracer()
        .spanBuilder("random-name-async")
        .setParent(otelCurrentContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan()
        .setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));

    CompletableFuture<RandomUserApi.Name> result = webClient.get()
        .retrieve()
        .bodyToMono(RandomUserApi.Name.class)
        .toFuture()
        .whenComplete((name, ex) -> {
          if (ex != null) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
          } else {
            span.setAttribute("random.name", name.name());
          }
          span.end();
        });

    return result.thenApply(RandomUserApi.Name::name);
  }
}
