/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A view service call handler for transforming updates.
 *
 * <p>This annotation should be placed on methods that handle View update service calls.
 *
 * <p>The first method parameter should correspond to the parameter in the service call.
 *
 * <p>A second parameter can optionally be defined for the previous state. It's type corresponds to
 * the return type of the service call. It can be defined as <code>Optional</code>. For the first
 * event of an event sourced entity or for the first change of a value entity there is no previous
 * state and then <code>Optional.empty</code> or <code>null</code> is used for the state parameter.
 *
 * <p>The method may also take a {@link UpdateHandlerContext} parameter.
 *
 * <p>The method should return the updated (new) state.
 */
@AkkaServerlessAnnotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateHandler {

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
