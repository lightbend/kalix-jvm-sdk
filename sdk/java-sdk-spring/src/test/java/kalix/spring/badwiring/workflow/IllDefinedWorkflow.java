/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.badwiring.workflow;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
import org.springframework.stereotype.Component;

@Id("id")
@TypeId("test")
@Component
public class IllDefinedWorkflow extends Workflow<String> {
  @Override
  public WorkflowDef<String> definition() {
    return null;
  }
}
