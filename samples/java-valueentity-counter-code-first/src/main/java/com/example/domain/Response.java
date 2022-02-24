package com.example.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {
  public final String message;


  @JsonCreator
  public Response(@JsonProperty("message") String msg) {
    this.message = msg;
  }

  public static Response done() {
    return new Response("done");
  }
}
