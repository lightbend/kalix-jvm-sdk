package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.SpringWebfluxTelemetry;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomPhotoActionAsync extends Action {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetRandomPhotoActionAsync.class);

  private final Tracer tracer;
  private final WebClient webClient;

  public GetRandomPhotoActionAsync(Tracer tracer, OpenTelemetry openTelemetry) {
    this.tracer = tracer;
    this.webClient = WebClient.create("https://randomuser.me/api/?inc=picture&noinfo");
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    var photoReply = getRandomPhotoAsync();
    var updatePhoto = photoReply.thenCompose(randomPhotoUrl -> {
      // Example for manually injecting headers for tracing context propagation
      // NOTE: tracing context is automatically propagated when using component client, no need to manually inject headers
      // in this case we are using WebClient directly instead, only for demonstration purposes but if you're doing a local call
      // you should use the component client instead
      var tracingMap = Map.of(
          "traceparent", actionContext().metadata().traceContext().traceParent().orElse(""),
          "tracestate", actionContext().metadata().traceContext().traceState().orElse("")
      );

      return WebClient.create("http://localhost:9000")
          .put()
          .uri(uriBuilder -> uriBuilder
              .path("/user/{userId}/photo")
              .queryParam("url", randomPhotoUrl)
              .build(actionContext().eventSubject().get()))
          .headers(h -> tracingMap.forEach(h::set))
          .retrieve()
          .bodyToMono(String.class)
          .toFuture();
    });

    return effects().asyncReply(updatePhoto);
  }

  // gets random name from external API using a synchronous call and traces that call ^
  private CompletableFuture<String> getRandomPhotoAsync() {
    Span span = tracer
        .spanBuilder("random-name-async")
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();

    try (Scope ignored = span.makeCurrent()) {

      return webClient
          .get()
          .retrieve()
          .bodyToMono(RandomUserApi.Photo.class)
          .map(photoResult -> {
            span.setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));
            span.setAttribute("random.photo", photoResult.url());
            span.end();
            return photoResult.url();
          }).doOnError(throwable -> {
            span.setStatus(StatusCode.ERROR, "Failed to fetch name: " + throwable.getMessage());
            span.end();
          }).toFuture();
    }

  }

}
