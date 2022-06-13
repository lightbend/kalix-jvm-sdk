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

package kalix.springsdk.valueentity;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.javasdk.valueentity.ValueEntityProvider;
import kalix.springsdk.impl.ComponentDescription;
import kalix.springsdk.impl.Introspector;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.reflection.NameGenerator;
import kalix.springsdk.impl.valueentity.ReflectiveValueEntityRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveValueEntityProvider<S, E extends ValueEntity<S>>
    implements ValueEntityProvider<S, E> {

  private final String entityType;
  private final Function<ValueEntityContext, E> factory;
  private final ValueEntityOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescription componentDescription;

  public static <S, E extends ValueEntity<S>> ReflectiveValueEntityProvider<S, E> of(
      String entityType, Class<E> cls, Function<ValueEntityContext, E> factory) {
    return new ReflectiveValueEntityProvider<>(
        entityType, cls, factory, ValueEntityOptions.defaults());
  }

  public ReflectiveValueEntityProvider(
      String entityType,
      Class<E> cls,
      Function<ValueEntityContext, E> factory,
      ValueEntityOptions options) {
    this.entityType = entityType;
    this.factory = factory;
    this.options = options;

    this.componentDescription = Introspector.inspect(cls);

    this.fileDescriptor = componentDescription.fileDescriptor();
    this.serviceDescriptor = componentDescription.serviceDescriptor();
  }

  @Override
  public ValueEntityOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public String entityType() {
    return entityType;
  }

  @Override
  public ValueEntityRouter<S, E> newRouter(ValueEntityContext context) {
    E entity = factory.apply(context);
    return new ReflectiveValueEntityRouter<>(entity, componentDescription.methods());
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {fileDescriptor};
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(SpringSdkMessageCodec.instance());
  }
}
