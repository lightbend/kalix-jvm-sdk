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


  @Override
  public Effect<ControllerActionApi.MessageResponse> callAsyncEndpoint(Empty empty) {
    // tag::get-tracer[]
    Optional<Tracer> tracerOpt = actionContext().getOpenTelemetryTracer();
    // end::get-tracer[]
    CompletableFuture<HttpResponse<Post>> futureResponse;

    if(tracerOpt.isPresent()) {
      futureResponse = callAsyncService(tracerOpt.get());
    } else {
      futureResponse = callAsyncService();
    }
    CompletionStage<ControllerActionApi.MessageResponse> asyncComputation = futureResponse.toCompletableFuture()
            .thenApply(response -> {
      return ControllerActionApi.MessageResponse.newBuilder().setMessage(response.body().title).build();
    });
    return effects().asyncReply(asyncComputation);
  }


  private CompletableFuture<HttpResponse<Post>> callAsyncService(){
    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
    //Async call to external service
    return httpClient.sendAsync(httpRequest,
            new JsonResponseHandler<>(Post.class));
  }


  // tag::create-close-span[]
  private CompletableFuture<HttpResponse<Post>> callAsyncService(Tracer tracer) {

    Span span  = tracer
            .spanBuilder("https://jsonplaceholder.typicode.com/posts/{}")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())// <1>
            .startSpan(); // <2>
    span.setAttribute("post", "1");// <3>

    CompletableFuture<HttpResponse<Post>> responseFuture = callAsyncService();

    responseFuture.thenAccept(response -> {
      span.setAttribute("result", response.body().title);// <5>
      span.end();// <6>
    }).exceptionally(ex -> {
      span.setStatus(StatusCode.ERROR, ex.getMessage());// <7>
      span.end();// <6>
      return null;
    });
    return responseFuture;
  }
  // end::create-close-span[]

  public record Post(String userId, String id, String title, String body) {}



}