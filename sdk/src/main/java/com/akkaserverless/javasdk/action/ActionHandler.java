/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.Reply;
import com.google.protobuf.Any;

import java.util.concurrent.CompletionStage;

/** Low level interface for handling for action calls. */
public interface ActionHandler {

  /**
   * Handle a unary call.
   *
   * @param commandName The name of the command this call is for.
   * @param message The message envelope of the message.
   * @param context The action context.
   * @return A future of the message to return.
   */
  CompletionStage<Reply<Any>> handleUnary(
      String commandName, MessageEnvelope<Any> message, ActionContext context);

  /**
   * Handle a streamed out call call.
   *
   * @param commandName The name of the command this call is for.
   * @param message The message envelope of the message.
   * @param context The action context.
   * @return The stream of messages to return.
   */
  Source<Reply<Any>, NotUsed> handleStreamedOut(
      String commandName, MessageEnvelope<Any> message, ActionContext context);

  /**
   * Handle a streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream The stream of messages to handle.
   * @param context The action context.
   * @return A future of the message to return.
   */
  CompletionStage<Reply<Any>> handleStreamedIn(
      String commandName, Source<MessageEnvelope<Any>, NotUsed> stream, ActionContext context);

  /**
   * Handle a full duplex streamed in call.
   *
   * @param commandName The name of the command this call is for.
   * @param stream The stream of messages to handle.
   * @param context The action context.
   * @return The stream of messages to return.
   */
  Source<Reply<Any>, NotUsed> handleStreamed(
      String commandName, Source<MessageEnvelope<Any>, NotUsed> stream, ActionContext context);
}
