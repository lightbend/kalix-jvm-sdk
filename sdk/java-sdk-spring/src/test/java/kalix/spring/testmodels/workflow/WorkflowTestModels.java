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

package kalix.spring.testmodels.workflow;

import kalix.javasdk.annotations.*;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class WorkflowTestModels {

  @Type("transfer-workflow")
  @Key("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithTypeLevelKey extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithMethodLevelKey extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @Key("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @Key("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithKeyOverridden extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @Key("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @Key("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithIllDefinedIdGenerator extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @GenerateEntityKey
    @Key("id")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithoutIdGeneratorAndEntityKey extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithIdGenerator extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @GenerateEntityKey
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithJWT extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @JWT(
        validate = JWT.JwtMethodMode.BEARER_TOKEN,
        bearerTokenIssuer = {"a", "b"})
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @Type("transfer-workflow")
  @Key("transferId")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class WorkflowWithAcl extends Workflow<WorkflowState> {

    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }
  }

  @Type("transfer-workflow")
  @Key("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithMethodLevelAcl extends Workflow<WorkflowState> {

    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @Acl(allow = @Acl.Matcher(service = "test"))
    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }
}
