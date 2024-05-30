package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import java.io.IOException;
import java.net.http.HttpResponse;

public class JsonResponseHandler<T> implements HttpResponse.BodyHandler<T> {
  private final Class<T> responseType;

  public JsonResponseHandler(Class<T> responseType){
    this.responseType = responseType;
  }

  @Override
  public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
    return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofString(Charsets.UTF_8), responseBody -> {
      try {
        return new ObjectMapper().readValue(responseBody, responseType);
      } catch (IOException e){
        throw new RuntimeException("Failed to parse JSON");
      }
    });
  }
}