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

package kalix.springsdk.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.action.ActionRouter;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.action.ActionIntrospector;
import kalix.springsdk.impl.action.ActionReflectiveRouter;
import kalix.springsdk.impl.action.ActionDescription;
import kalix.springsdk.impl.reflection.NameGenerator;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveActionProvider<A extends Action> implements ActionProvider<A> {

  private final Function<ActionCreationContext, A> factory;

  private final ActionOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ActionDescription<A> actionDescription;

  public static <A extends Action> ReflectiveActionProvider<A> of(
      Class<A> cls, Function<ActionCreationContext, A> factory) {
    return new ReflectiveActionProvider<>(cls, factory, ActionOptions.defaults());
  }

  private ReflectiveActionProvider(
      Class<A> cls, Function<ActionCreationContext, A> factory, ActionOptions options) {

    this.factory = factory;
    this.options = options;

    this.actionDescription =
        ActionIntrospector.inspect(cls, new NameGenerator(), new ObjectMapper());

    this.fileDescriptor = actionDescription.fileDescriptor();
    this.serviceDescriptor = actionDescription.serviceDescriptor();
  }

  @Override
  public ActionOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public ActionRouter<A> newRouter(ActionCreationContext context) {
    A action = factory.apply(context);
    return new ActionReflectiveRouter<>(action, actionDescription.methods());
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
