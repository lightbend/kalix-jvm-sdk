/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import com.google.protobuf.Descriptors;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.workflow.WorkflowRouter;
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
  private final String typeId;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public ReflectiveWorkflowProvider(Class<W> workflowClass, JsonMessageCodec messageCodec, Function<WorkflowContext, W> factory, WorkflowOptions options) {
    TypeId annotation = workflowClass.getAnnotation(TypeId.class);
    if (annotation == null) {
      throw new IllegalArgumentException(
          "Workflow [" + workflowClass.getName() + "] is missing '@Type' annotation");
    }

    this.workflowClass = workflowClass;
    this.messageCodec = new StrictJsonMessageCodec(messageCodec);
    this.factory = factory;
    this.options = options;
    this.typeId = annotation.value();
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
  public String typeId() {
    return typeId;
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
