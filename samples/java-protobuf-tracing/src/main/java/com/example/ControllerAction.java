package com.example;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ControllerAction extends AbstractControllerAction {

  String url = "https://jsonplaceholder.typicode.com/posts/1";
  HttpClient httpClient = HttpClient.newHttpClient();
  public ControllerAction(ActionCreationContext creationContext) {}


  @Override
  public Effect<ControllerActionApi.MessageResponse> callSyncEndpoint(Empty empty) {
    //Taking the already configured tracer. Such it will know where to export the spans
    Tracer tracer = actionContext().getOpenTelemetryTracer().get();
    String result = callSyncService(tracer);
    return effects().reply(ControllerActionApi.MessageResponse.newBuilder().setMessage(result).build());
  }


  @Override
  public Effect<ControllerActionApi.MessageResponse> callAsyncEndpoint(Empty empty) {
    Tracer tracer = actionContext().getOpenTelemetryTracer().get();

    CompletionStage<ControllerActionApi.MessageResponse> asyncComputation = callAsyncService(tracer).toCompletableFuture().thenApply(response -> {
      return ControllerActionApi.MessageResponse.newBuilder().setMessage(response.body()).build();
    });
    return effects().asyncReply(asyncComputation);
  }


  private CompletableFuture<HttpResponse<String>> callAsyncService(Tracer tracer) {

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();

    Span span  = tracer
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())
            .startSpan();
    span.setAttribute("attribute1", "value1");

    //Async call to external service
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
        return null; //CompletableFuture<Void>
      });
    }
    return responseFuture;
  }

  private String callSyncService(Tracer tracer)  {
    Span span  = tracer
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())
            .startSpan();
    span.setAttribute("attribute1", "value1");

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();


    String result = "";
    //scope must be closed. try-with-resources will automatically close it.
    try(Scope scope = span.makeCurrent()){
      //Sync call to external service
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      result = response.body();
      span.setAttribute("result", result);
    } catch (IOException | InterruptedException e) {
      result = e.getMessage();
      span.setStatus(StatusCode.ERROR, result);
    } finally {
      span.end();
    }
    return result;
  }



}
