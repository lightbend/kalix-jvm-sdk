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

package kalix.javasdk.action;

import kalix.serializer.Serializer;
import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.ProtoDescriptorGenerator;
import kalix.javasdk.impl.action.ActionReflectiveRouter;
import kalix.javasdk.impl.action.ActionRouter;

import java.util.Map;
import java.util.function.Function;

public class ReflectiveActionProvider<A extends Action> implements ActionProvider<A> {

  private final Function<ActionCreationContext, A> factory;

  private final ActionOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;

  public static <A extends Action> ReflectiveActionProvider<A> of(Class<A> cls, Function<ActionCreationContext, A> factory) {
    return new ReflectiveActionProvider<>(cls, factory, ActionOptions.defaults());
  }

  private ReflectiveActionProvider(Class<A> cls, Function<ActionCreationContext, A> factory, ActionOptions options) {
    this.factory = factory;
    this.options = options;
    this.fileDescriptor = ProtoDescriptorGenerator.generateFileDescriptorAction(cls);
    this.serviceDescriptor = fileDescriptor.findServiceByName(cls.getSimpleName());
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
    return new ActionReflectiveRouter<>(action);
  }


  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[]{
        fileDescriptor
    };
  }

  @Override
  public Map<Class<?>, Serializer> additionalSerializers() {
    return Serializer.buildSerializersJava(getClass().getClassLoader(), fileDescriptor);
  }
}