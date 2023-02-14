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

package kalix.javasdk.workflowentity;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.workflowentity.WorkflowEntityRouter;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.StrictJsonMessageCodec;
import kalix.javasdk.impl.workflow.ReflectiveWorkflowEntityRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveWorkflowEntityProvider<S, W extends WorkflowEntity<S>> implements WorkflowEntityProvider<S, W> {

  private final Class<W> workflowClass;
  private final MessageCodec messageCodec;
  private final Function<WorkflowEntityContext, W> factory;
  private final WorkflowEntityOptions options;
  private final String entityType;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public ReflectiveWorkflowEntityProvider(Class<W> workflowClass, JsonMessageCodec messageCodec, Function<WorkflowEntityContext, W> factory, WorkflowEntityOptions options) {
    EntityType annotation = workflowClass.getAnnotation(EntityType.class);
    if (annotation == null) {
      throw new IllegalArgumentException(
          "Workflow Entity [" + workflowClass.getName() + "] is missing '@EntityType' annotation");
    }

    this.workflowClass = workflowClass;
    this.messageCodec = new StrictJsonMessageCodec(messageCodec);
    this.factory = factory;
    this.options = options;
    this.entityType = annotation.value();
    this.componentDescriptor = ComponentDescriptor.descriptorFor(workflowClass, messageCodec);
    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  public static <S, W extends WorkflowEntity<S>> ReflectiveWorkflowEntityProvider<S, W> of(
      Class<W> cls,
      JsonMessageCodec messageCodec,
      Function<WorkflowEntityContext, W> factory) {
    return new ReflectiveWorkflowEntityProvider<>(
        cls, messageCodec, factory, WorkflowEntityOptions.defaults());
  }

  @Override
  public String workflowName() {
    return entityType;
  }

  @Override
  public WorkflowEntityOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public WorkflowEntityRouter<S, W> newRouter(WorkflowEntityContext context) {
    W workflow = factory.apply(context);
    return new ReflectiveWorkflowEntityRouter<>(
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
