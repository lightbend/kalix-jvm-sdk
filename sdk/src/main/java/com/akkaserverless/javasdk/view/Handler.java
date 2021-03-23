/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A view service call handler.
 *
 * <p>This annotation should be placed on methods that handle View service calls.
 *
 * <p>FIXME: what types of the input and output parameters are supported. unary/streamed? see
 * valueentity.CommandHandler
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
