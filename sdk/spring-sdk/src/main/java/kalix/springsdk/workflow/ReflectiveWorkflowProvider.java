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

package kalix.springsdk.workflow;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.workflow.WorkflowRouter;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import kalix.javasdk.workflow.WorkflowOptions;
import kalix.javasdk.workflow.WorkflowProvider;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.impl.ComponentDescriptor;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.workflow.ReflectiveWorkflowRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveWorkflowProvider<S, W extends Workflow<S>> implements WorkflowProvider<S, W> {

  private final Class<W> workflowClass;
  private final SpringSdkMessageCodec messageCodec;
  private final Function<WorkflowContext, W> factory;
  private final WorkflowOptions options;
  private final String workflowType;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public ReflectiveWorkflowProvider(Class<W> workflowClass, SpringSdkMessageCodec messageCodec, Function<WorkflowContext, W> factory, WorkflowOptions options) {
    EntityType annotation = workflowClass.getAnnotation(EntityType.class);
    if (annotation == null)
      throw new IllegalArgumentException(
          "Event Sourced Entity [" + workflowClass.getName() + "] is missing '@EntityType' annotation");

    this.workflowClass = workflowClass;
    this.messageCodec = messageCodec;
    this.factory = factory;
    this.options = options;
    this.workflowType = annotation.value();
    this.componentDescriptor = ComponentDescriptor.descriptorFor(workflowClass, messageCodec);
    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  public static <S, W extends Workflow<S>> ReflectiveWorkflowProvider<S, W> of(
      Class<W> cls,
      SpringSdkMessageCodec messageCodec,
      Function<WorkflowContext, W> factory) {
    return new ReflectiveWorkflowProvider<>(
        cls, messageCodec, factory, WorkflowOptions.defaults());
  }

  @Override
  public String workflowName() {
    return workflowType;
  }

  @Override
  public WorkflowOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public WorkflowRouter<S, W> newRouter(WorkflowContext context) {
    W workflow = factory.apply(context);
    return new ReflectiveWorkflowRouter<>(
        workflow, componentDescriptor.commandHandlers(), messageCodec);
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
