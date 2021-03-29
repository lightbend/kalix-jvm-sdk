/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.Jsonable;

@Jsonable
public class JsonMessage {
  public JsonMessage(String message) {
    this.message = message;
  }

  public JsonMessage() {}

  public String message;
}
