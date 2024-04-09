/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.workflow.WorkflowRouter;

import java.util.Optional;

public interface WorkflowProvider<S, W extends AbstractWorkflow<S>> {

  String typeId();

  WorkflowOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  WorkflowRouter<S, W> newRouter(WorkflowContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }

}
