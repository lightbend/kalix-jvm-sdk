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
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.impl.client.ComponentCall;
import kalix.javasdk.impl.client.ComponentCall2;
import kalix.javasdk.valueentity.ValueEntity;

import java.util.Optional;

public class ValueEntityCallBuilder {

  private final KalixClient kalixClient;
  private final Optional<String> entityId;

  public ValueEntityCallBuilder(KalixClient kalixClient, String entityId) {
    this.kalixClient = kalixClient;
    this.entityId = Optional.of(entityId);
  }

  public ValueEntityCallBuilder(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
    this.entityId = Optional.empty();
  }

  public <T, R> DeferredCall<Any, R> call(Function<T, ValueEntity.Effect<R>> methodRef) {
    return ComponentCall.noParams(kalixClient, methodRef, entityId);
  }

  public <T, A1, R> ComponentCall<A1, R> call(Function2<T, A1, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall(kalixClient, methodRef, entityId);
  }

  public <T, A1, A2, R> ComponentCall2<A1, A2, R> call(Function3<T, A1, A2, ValueEntity.Effect<R>> methodRef) {
    return new ComponentCall2(kalixClient, methodRef, entityId);
  }
}
