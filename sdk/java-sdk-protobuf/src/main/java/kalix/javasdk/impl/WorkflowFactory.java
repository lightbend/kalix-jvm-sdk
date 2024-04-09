/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.impl.workflow.WorkflowRouter;
import kalix.javasdk.workflow.WorkflowContext;

public interface WorkflowFactory {

  WorkflowRouter<?, ?> create(WorkflowContext context);
}
