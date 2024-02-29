package com.example;

import com.google.protobuf.Empty;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.ActionCreationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/controller_action.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ControllerAction extends AbstractControllerAction {

  public ControllerAction(ActionCreationContext creationContext) {}


  @Override
  public Effect<ControllerActionApi.MessageResponse> callSyncEndpoint(Empty empty) {
    Span span  = actionContext().getOpenTelemetryTracer().get()
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())
            .startSpan();
    span.setAttribute("attribute1", "value1");
    String result = "";
    //scope automatically closes, try-with-resources
    try (Scope scope = span.makeCurrent()) {
      result = callSyncService(span);
      span.setAttribute("result", result);
    } catch (IOException | InterruptedException e) {
      result = e.getMessage();
      span.setStatus(StatusCode.ERROR, result);
    } finally {
      span.end();
    }
    return effects().reply(ControllerActionApi.MessageResponse.newBuilder().setMessage(result).build());
  }


  @Override
  public Effect<ControllerActionApi.MessageResponse> callAsyncEndpoint(Empty empty) {
    Span span  = actionContext().getOpenTelemetryTracer().get()
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())
            .startSpan();
    span.setAttribute("attribute1", "value1");
    CompletionStage<ControllerActionApi.MessageResponse> asyncComputation = callAsyncService(span).toCompletableFuture().thenApply(response -> {
      return ControllerActionApi.MessageResponse.newBuilder().setMessage(response.body()).build();
    });
    return effects().asyncReply(asyncComputation);
  }


  private CompletableFuture<HttpResponse<String>> callAsyncService(Span span) {
    HttpClient httpClient = HttpClient.newHttpClient();

    String url = "https://jsonplaceholder.typicode.com/posts/1";

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();

    CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(httpRequest,
            HttpResponse.BodyHandlers.ofString());
    try (Scope scope = span.makeCurrent()) {
      responseFuture.thenAccept(response -> {
        String responseBody = response.body();
        span.setAttribute("result", responseBody);
        span.end();
      }).exceptionally(ex -> {
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        span.end();
        return null;
      });
    }
    return responseFuture;
  }

  private String callSyncService(Span span) throws IOException, InterruptedException {
    String url = "https://jsonplaceholder.typicode.com/posts/1";

    HttpClient httpGet = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("traceparent", span.getSpanContext().getTraceId()) // you could pass the traceparent to the next service if you'd like to continue the trace
            .build();
    HttpResponse<String> response = httpGet.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }



}
