package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.protobuf.Empty;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.ActionCreationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ControllerAction extends AbstractControllerAction {

  String url = "https://jsonplaceholder.typicode.com/posts/1";
  HttpClient httpClient = HttpClient.newHttpClient();
  public ControllerAction(ActionCreationContext creationContext) {}

  // tag::create-close-span[]
  @Override
  public Effect<ControllerActionApi.MessageResponse> callAsyncEndpoint(Empty empty) {
    // tag::get-tracer[]
    Optional<Tracer> tracerOpt = actionContext().getOpenTelemetryTracer();
    // end::get-tracer[]

    Optional<Span> span = tracerOpt.map(tracer -> {
      return tracer
              .spanBuilder("https://jsonplaceholder.typicode.com/posts/{}")
              .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())// <1>
              .startSpan() // <2>
              .setAttribute("post", "1");// <3>
    });

    CompletableFuture<ControllerActionApi.MessageResponse> asyncComputation = callAsyncService()
      .whenComplete((response, ex) -> {
        if (ex != null) {
          span.ifPresent(presentSpan ->
            presentSpan
              .setStatus(StatusCode.ERROR, ex.getMessage())// <4>
              .end());// <5>
        } else {
          span.ifPresent(presentSpan ->
            presentSpan
              .setAttribute("result", response.body().title)// <3>
              .end());// <5>
        }
      })
      .thenApply(response ->
          ControllerActionApi.MessageResponse.newBuilder().setMessage(response.body().title).build()
      );
    return effects().asyncReply(asyncComputation);
  }
  // end::create-close-span[]


  private CompletableFuture<HttpResponse<Post>> callAsyncService(){
    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
    //Async call to external service
    return httpClient.sendAsync(httpRequest,
            new JsonResponseHandler<>(Post.class));
  }

  public record Post(String userId, String id, String title, String body) {}



}