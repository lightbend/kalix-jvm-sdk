package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomPhotoAction extends Action {
  private static final Logger log = LoggerFactory.getLogger(GetRandomPhotoAction.class);

  private final Tracer tracer;
  private final WebClient webClient;

  public GetRandomPhotoAction(Tracer tracer) {
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

  // gets random name from external API using an asynchronous call and traces that call
  private CompletableFuture<String> getRandomPhotoAsync() {
    var otelCurrentContext = actionContext().metadata().traceContext().asOpenTelemetryContext();
    Span span = tracer
        .spanBuilder("random-photo-async")
        .setParent(otelCurrentContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan()
        .setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));

    return webClient
        .get()
        .retrieve()
        .bodyToMono(RandomUserApi.Photo.class)
        .map(photoResult -> {
          span.setAttribute("random.photo", photoResult.url());
          span.end();
          return photoResult.url();
        }).doOnError(throwable -> {
          span.setStatus(StatusCode.ERROR, "Failed to fetch name: " + throwable.getMessage());
          span.end();
        }).toFuture();
  }

}
