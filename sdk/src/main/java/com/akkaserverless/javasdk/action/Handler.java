/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;
import com.akkaserverless.javasdk.Reply;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An action service call handler.
 *
 * <p>This annotation should be placed on methods that handle Action service calls.
 *
 * <p>The types of the input and output parameters for these methods depend on whether the call is a
 * unary or streamed call.
 *
 * <p>Calls with a unary in argument may accept the protobuf type of the call, either bare, or
 * wrapped in {@link MessageEnvelope}.
 *
 * <p>Calls with a streamed in argument may accept either a {@link akka.stream.javadsl.Source} or a
 * {@link org.reactivestreams.Publisher}. The element type may either be the bare protobuf type of
 * the call, or that type wrapped in {@link MessageEnvelope}.
 *
 * <p>Calls with a unary out argument may either return synchronously, or return a {@link
 * java.util.concurrent.CompletionStage}. The argument return type may either be the raw protobuf
 * output type of the call, or wrapped in {@link MessageEnvelope} or {@link Reply}.
 *
 * <p>Calls with a streamed out argument may either return a {@link akka.stream.javadsl.Source} or a
 * {@link org.reactivestreams.Publisher}. The element type of these may either be the raw protobuf
 * output type of the call, or wrapped in {@link MessageEnvelope} or {@link Reply}.
 *
 * <p>The method may also take an {@link ActionContext}.
 */
@AkkaServerlessAnnotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {

  /**
   * The name of the service call to handle.
   *
   * <p>If not specified, the name of the method will be used as the service call name, with the
   * first letter capitalized to match the gRPC convention of capitalizing rpc method names.
   *
   * @return The service call name.
   */
  String name() default "";
}
