/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import kalix.javasdk.impl.workflow.WorkflowOptionsImpl;

public interface WorkflowOptions extends kalix.javasdk.impl.ComponentOptions {

  static WorkflowOptions defaults() {
    return WorkflowOptionsImpl.defaults();
  }
}
