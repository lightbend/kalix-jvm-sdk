/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

public class JsonMessage {
  public JsonMessage(String message) {
    this.message = message;
  }

  public JsonMessage() {}

  public String message;
}
