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

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Descriptors;

import java.util.function.Function;

public class LocalPersistenceSubscriberProvider
    implements ActionProvider<LocalPersistenceSubscriber> {

  private final Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory;
  private final ActionOptions options;

  private LocalPersistenceSubscriberProvider(
      Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory,
      ActionOptions options) {
    this.actionFactory = actionFactory;
    this.options = options;
  }

  public static LocalPersistenceSubscriberProvider of(
      Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory) {
    return new LocalPersistenceSubscriberProvider(actionFactory, ActionOptions.defaults());
  }

  @Override
  public final ActionOptions options() {
    return options;
  }

  public final LocalPersistenceSubscriberProvider withOptions(ActionOptions options) {
    return new LocalPersistenceSubscriberProvider(actionFactory, options);
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor()
        .findServiceByName("LocalPersistenceSubscriberModel");
  }

  @Override
  public LocalPersistenceSubscriberRouter newRouter(ActionCreationContext context) {
    return new LocalPersistenceSubscriberRouter(actionFactory.apply(context));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {LocalPersistenceEventing.getDescriptor()};
  }
}
