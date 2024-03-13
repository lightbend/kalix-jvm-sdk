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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ControllerAction extends AbstractControllerAction {

  String url = "https://jsonplaceholder.typicode.com/posts/1";
  HttpClient httpClient = HttpClient.newHttpClient();
  public ControllerAction(ActionCreationContext creationContext) {}


  @Override
  public Effect<ControllerActionApi.MessageResponse> callSyncEndpoint(Empty empty) {
    //Taking the already configured tracer. Such it will know where to export the spans
    Optional<Tracer> tracerOpt = actionContext().getOpenTelemetryTracer();
    String result;

    if (tracerOpt.isPresent()){
      result = callSyncService(tracerOpt.get());
    } else {
      try {
        HttpResponse<String> response = callSyncService();
        result = response.body();
      } catch (IOException | InterruptedException e){
        result = e.getMessage();
      }
    }
    return effects().reply(ControllerActionApi.MessageResponse.newBuilder().setMessage(result).build());
  }

  private  HttpResponse<String> callSyncService() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String callSyncService(Tracer tracer) {
    Span span = tracer
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())
            .startSpan();
    span.setAttribute("attribute1", "value1");

    String result = "";
    //scope must be closed. try-with-resources will automatically close it.
    try (Scope scope = span.makeCurrent()) {
      //Sync call to external service
      HttpResponse<String> response = callSyncService();
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


    @Override
  public Effect<ControllerActionApi.MessageResponse> callAsyncEndpoint(Empty empty) {
      // tag::get-tracer[]
      Optional<Tracer> tracerOpt = actionContext().getOpenTelemetryTracer();
      // end::get-tracer[]
      CompletableFuture<HttpResponse<String>> futureResponse;

    if(tracerOpt.isPresent()) {
      futureResponse = callAsyncService(tracerOpt.get());
    } else {
      futureResponse = callAsyncService();
    }
    CompletionStage<ControllerActionApi.MessageResponse> asyncComputation = futureResponse.toCompletableFuture().thenApply(response -> {
      return ControllerActionApi.MessageResponse.newBuilder().setMessage(response.body()).build();
    });
    return effects().asyncReply(asyncComputation);
  }


  private CompletableFuture<HttpResponse<String>> callAsyncService(){
    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
    //Async call to external service
    return httpClient.sendAsync(httpRequest,
            HttpResponse.BodyHandlers.ofString());

  }


  //  tag::create-close-span[]
  private CompletableFuture<HttpResponse<String>> callAsyncService(Tracer tracer) {
  // end::create-close-span[]

    // tag::create-close-span[]
    Span span  = tracer 
            .spanBuilder("loreipsumendpoint")
            .setParent(actionContext().metadata().traceContext().asOpenTelemetryContext())// <1>
            .startSpan(); // <2>
    span.setAttribute("attribute1", "value1");// <3>

    CompletableFuture<HttpResponse<String>> responseFuture = callAsyncService();

    try (Scope scope = span.makeCurrent()) {// <4>
      responseFuture.thenAccept(response -> {
        String responseBody = response.body();
        span.setAttribute("result", responseBody);// <5>
      }).exceptionally(ex -> {
        span.setStatus(StatusCode.ERROR, ex.getMessage());
        return null;
      });
    } finally {
      span.end();// <6>
    }
    return responseFuture;
  }
  // end::create-close-span[]




}
