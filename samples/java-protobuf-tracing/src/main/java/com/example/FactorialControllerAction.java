package com.example;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import kalix.javasdk.action.ActionCreationContext;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.example.FactorialActionApi.MessageResponse;
import com.google.protobuf.Empty;


// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/factorial_action.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FactorialControllerAction extends AbstractFactorialControllerAction {
  public FactorialControllerAction(ActionCreationContext creationContext) {
  }


  @Override
  public Effect<MessageResponse> callSyncEndpoint(Empty empty) {
    Span span  = actionContext().getOpenTelemetryTracer().get().spanBuilder("calculateFactorial").startSpan();
    span.setAttribute("attribute1", "value1");
    String result = "";
    try {
      result = callSyncService(span.getSpanContext().getTraceId()); 
      span.setAttribute("result", result); //pick some value from it
      span.end();
    } catch (IOException | InterruptedException e) {
      result = e.getMessage();
      span.setStatus(StatusCode.ERROR, e.getMessage());
      span.end();
    }
    return effects().reply(MessageResponse.newBuilder().setMessage(result).build());
  }


  @Override
	public Effect<MessageResponse> callAsyncEndpoint(Empty empty) {
    Span span  = actionContext().getOpenTelemetryTracer().get().spanBuilder("calculateFactorial").startSpan();
    span.setAttribute("attribute1", "value1");
    CompletionStage<MessageResponse>  asyncComputation = callAsyncService(span).toCompletableFuture().thenApply(response -> {
      return MessageResponse.newBuilder().setMessage(response.body()).build();
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

        responseFuture.thenAccept(response -> {
            String responseBody = response.body();
            span.setAttribute("result", responseBody);
            span.end();
        }).exceptionally(ex -> {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.end();
            return null;
        });
        
		return responseFuture;
  }

  private String callSyncService(String traceparent) throws IOException, InterruptedException {
        String url = "https://jsonplaceholder.typicode.com/posts/1";

        HttpClient httpGet = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("traceparent", traceparent)
                .build();
            HttpResponse<String> response = httpGet.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
    }



	

}


