package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomNameActionAsync extends Action {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetRandomNameActionAsync.class);

  private final Tracer tracer;
  private final WebClient webClient;

  public GetRandomNameActionAsync(ActionCreationContext context) {
    this.tracer = context.getTracer().orElse(null);
    this.webClient = WebClient.create("https://randomuser.me/api/?inc=name&noinfo");
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    var asyncReply = getRandomNameAsync();
    return effects().asyncReply(asyncReply);
  }

  private CompletableFuture<String> getRandomNameAsync() {
    Span span = startSpan("random-name-async");
    try (Scope ignored = span.makeCurrent()) {

      return webClient
          .get()
          .retrieve()
          .bodyToMono(RandomNameResult.class)
          .map(name -> {
            span.setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));
            span.setAttribute("random.name", name.name());
            span.end();
            return name.name();
          }).doOnError(throwable -> {
            span.setStatus(StatusCode.ERROR, "Failed to fetch name: " + throwable.getMessage());
            span.end();
          }).toFuture();
    }

  }

  private Span startSpan(String spanName) {
    var extractedContext = actionContext().metadata().traceContext().get();

    return tracer
        .spanBuilder(spanName)
        .setParent(extractedContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();
  }

}
