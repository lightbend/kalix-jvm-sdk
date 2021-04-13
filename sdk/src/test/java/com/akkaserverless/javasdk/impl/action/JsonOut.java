/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.action;

import com.akkaserverless.javasdk.Jsonable;

@Jsonable
public class JsonOut {
  public JsonOut(String message) {
    this.message = message;
  }

  public JsonOut() {}

  public String message;
}
