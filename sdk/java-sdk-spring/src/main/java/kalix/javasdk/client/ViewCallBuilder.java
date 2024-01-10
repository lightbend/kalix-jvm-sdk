/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.client;

import akka.japi.function.Function;
import akka.japi.function.Function10;
import akka.japi.function.Function11;
import akka.japi.function.Function12;
import akka.japi.function.Function13;
import akka.japi.function.Function14;
import akka.japi.function.Function15;
import akka.japi.function.Function16;
import akka.japi.function.Function17;
import akka.japi.function.Function18;
import akka.japi.function.Function19;
import akka.japi.function.Function2;
import akka.japi.function.Function20;
import akka.japi.function.Function21;
import akka.japi.function.Function22;
import akka.japi.function.Function3;
import akka.japi.function.Function4;
import akka.japi.function.Function5;
import akka.japi.function.Function6;
import akka.japi.function.Function7;
import akka.japi.function.Function8;
import akka.japi.function.Function9;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.impl.client.MethodRefResolver;
import kalix.javasdk.impl.client.ViewCallValidator;
import kalix.spring.KalixClient;

import java.lang.reflect.Method;
import java.util.List;

public class ViewCallBuilder {

  private final KalixClient kalixClient;

  public ViewCallBuilder(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, R> DeferredCall<Any, R> call(Function<T, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return ComponentCall.noParams(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, R> ComponentCall<A1, R> call(Function2<T, A1, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, R> ComponentCall2<A1, A2, R> call(Function3<T, A1, A2, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall2<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, R> ComponentCall3<A1, A2, A3, R> call(Function4<T, A1, A2, A3, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall3<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, R> ComponentCall4<A1, A2, A3, A4, R> call(Function5<T, A1, A2, A3, A4, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall4<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, R> ComponentCall5<A1, A2, A3, A4, A5, R> call(Function6<T, A1, A2, A3, A4, A5, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall5<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, R> ComponentCall6<A1, A2, A3, A4, A5, A6, R> call(Function7<T, A1, A2, A3, A4, A5, A6, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall6<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, R> ComponentCall7<A1, A2, A3, A4, A5, A6, A7, R> call(Function8<T, A1, A2, A3, A4, A5, A6, A7, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall7<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, R> ComponentCall8<A1, A2, A3, A4, A5, A6, A7, A8, R> call(Function9<T, A1, A2, A3, A4, A5, A6, A7, A8, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall8<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, R> ComponentCall9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> call(Function10<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall9<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> ComponentCall10<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> call(Function11<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall10<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> ComponentCall11<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> call(Function12<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall11<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> ComponentCall12<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> call(Function13<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall12<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> ComponentCall13<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> call(Function14<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall13<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> ComponentCall14<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> call(Function15<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall14<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> ComponentCall15<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> call(Function16<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall15<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> ComponentCall16<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> call(Function17<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall16<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> ComponentCall17<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> call(Function18<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall17<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> ComponentCall18<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> call(Function19<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall18<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> ComponentCall19<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> call(Function20<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall19<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> ComponentCall20<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> call(Function21<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall20<>(kalixClient, method, List.of());
  }

  /**
   * Pass in a View method reference annotated as a REST endpoint, e.g. <code>UserByCity::find</code>
   */
  public <T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> ComponentCall21<A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> call(Function22<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R> methodRef) {
    Method method = MethodRefResolver.resolveMethodRef(methodRef);
    ViewCallValidator.validate(method);
    return new ComponentCall21<>(kalixClient, method, List.of());
  }
}
