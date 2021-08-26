/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * <p>The method may also take a {@link UpdateContext} parameter.
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
