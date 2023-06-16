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

package kalix.spring;

import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.japi.function.Function3;
import akka.japi.function.Function4;
import kalix.javasdk.action.Action;
import kalix.javasdk.impl.client.ComponentCall;
import kalix.javasdk.impl.client.ComponentCall2;
import kalix.javasdk.impl.client.ComponentCall3;

import java.util.Optional;

public class ActionCallBuilder {

  private final KalixClient kalixClient;

  public ActionCallBuilder(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  public <T, R> ComponentCall<T, R> call(Function<T, Action.Effect<R>> methodRef) {
    return new ComponentCall(kalixClient, methodRef, Optional.empty());
  }

  public <T, A1, R> ComponentCall<A1, R> call(Function2<T, A1, Action.Effect<R>> methodRef) {
    return new ComponentCall(kalixClient, methodRef, Optional.empty());
  }

  public <T, A1, A2, R> ComponentCall2<A1, A2, R> call(Function3<T, A1, A2, Action.Effect<R>> methodRef) {
    return new ComponentCall2(kalixClient, methodRef, Optional.empty());
  }

  public <T, A1, A2, A3, R> ComponentCall3<A1, A2, A3, R> call(Function4<T, A1, A2, A3, Action.Effect<R>> methodRef) {
    return new ComponentCall3(kalixClient, methodRef, Optional.empty());
  }
}
