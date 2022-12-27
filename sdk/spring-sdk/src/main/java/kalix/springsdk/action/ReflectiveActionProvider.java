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

import com.google.protobuf.Descriptors;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.action.ActionRouter;
import kalix.springsdk.common.ForwardHeadersExtractor;
import kalix.springsdk.impl.ComponentDescriptor;
import kalix.springsdk.impl.ComponentDescriptorFactory;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.action.ReflectiveActionRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveActionProvider<A extends Action> implements ActionProvider<A> {

  private final Function<ActionCreationContext, A> factory;

  private final ActionOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;
  private final SpringSdkMessageCodec messageCodec;

  public static <A extends Action> ReflectiveActionProvider<A> of(
      Class<A> cls,
      SpringSdkMessageCodec messageCodec,
      Function<ActionCreationContext, A> factory) {
    return new ReflectiveActionProvider<>(cls, messageCodec, factory, ActionOptions.defaults());
  }

  private ReflectiveActionProvider(
      Class<A> cls,
      SpringSdkMessageCodec messageCodec,
      Function<ActionCreationContext, A> factory,
      ActionOptions options) {

    this.factory = factory;
    this.options = options.withForwardHeaders(ForwardHeadersExtractor.extractFrom(cls));
    this.messageCodec = messageCodec;

    this.componentDescriptor = ComponentDescriptor.descriptorFor(cls, messageCodec);

    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
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
    return new ReflectiveActionRouter<>(action, componentDescriptor.commandHandlers(), ComponentDescriptorFactory.findIgnore(action.getClass()));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {fileDescriptor};
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(messageCodec);
  }
}
