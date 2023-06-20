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

package kalix.javasdk.workflow;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.workflow.WorkflowRouter;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.StrictJsonMessageCodec;
import kalix.javasdk.impl.workflow.ReflectiveWorkflowRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveWorkflowProvider<S, W extends Workflow<S>> implements WorkflowProvider<S, W> {

  private final Class<W> workflowClass;
  private final MessageCodec messageCodec;
  private final Function<WorkflowContext, W> factory;
  private final WorkflowOptions options;
  private final String workflowType;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public ReflectiveWorkflowProvider(Class<W> workflowClass, JsonMessageCodec messageCodec, Function<WorkflowContext, W> factory, WorkflowOptions options) {
    EntityType annotation = workflowClass.getAnnotation(EntityType.class);
    if (annotation == null) {
      throw new IllegalArgumentException(
          "Workflow [" + workflowClass.getName() + "] is missing '@EntityType' annotation");
    }

    this.workflowClass = workflowClass;
    this.messageCodec = new StrictJsonMessageCodec(messageCodec);
    this.factory = factory;
    this.options = options;
    this.workflowType = annotation.value();
    this.componentDescriptor = ComponentDescriptor.descriptorFor(workflowClass, messageCodec);
    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  public static <S, W extends Workflow<S>> ReflectiveWorkflowProvider<S, W> of(
      Class<W> cls,
      JsonMessageCodec messageCodec,
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
        workflow, componentDescriptor.commandHandlers());
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
