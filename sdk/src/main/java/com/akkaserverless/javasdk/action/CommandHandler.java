/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An action call handler.
 *
 * <p>This annotation should be placed on methods that handle Action service calls.
 *
 * <p>The types of the input and output parameters for these methods depend on whether the call is a
 * unary or streamed call.
 *
 * <p>Calls with a unary in argument may accept the protobuf type of the call, either bare, or
 * wrapped in {@link MessageEnvelope}.
 *
 * <p>Calls with a streamed in argument may accept either a {@link akka.stream.javadsl.Source},
 * {@link org.reactivestreams.Publisher} or a {@link java.util.concurrent.Flow.Publisher}. The
 * element type may either be the bare protobuf type of the call, or that type wrapped in {@link
 * MessageEnvelope}.
 *
 * <p>Calls with a unary out argument may either return synchronously, or return a {@link
 * java.util.concurrent.CompletionStage}. The argument return type may either be the raw protobuf
 * output type of the call, or wrapped in {@link MessageEnvelope} or {@link ActionReply}.
 *
 * <p>Calls with a streamed out argument may either return a {@link akka.stream.javadsl.Source},
 * {@link org.reactivestreams.Publisher} or a {@link java.util.concurrent.Flow.Publisher}. The
 * element type of these may either be the raw protobuf output type of the call, or wrapped in
 * {@link MessageEnvelope} or {@link ActionReply}.
 */
@AkkaServerlessAnnotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandHandler {

  /**
   * The name of the command to handle.
   *
   * <p>If not specified, the name of the method will be used as the command name, with the first
   * letter capitalized to match the gRPC convention of capitalizing rpc method names.
   *
   * @return The command name.
   */
  String name() default "";
}
