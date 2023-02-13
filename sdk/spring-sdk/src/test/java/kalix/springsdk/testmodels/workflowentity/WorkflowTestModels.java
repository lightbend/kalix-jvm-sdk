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

package kalix.springsdk.testmodels.workflowentity;

import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.GenerateEntityKey;
import kalix.javasdk.annotations.JWT;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class WorkflowTestModels {

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithTypeLevelKey extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithMethodLevelKey extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @EntityKey("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @EntityKey("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithKeyOverridden extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @EntityKey("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @EntityKey("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithIllDefinedIdGenerator extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @GenerateEntityKey
    @EntityKey("id")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithoutIdGeneratorAndEntityKey extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithIdGenerator extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
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
  public static class WorkflowEntityWithJWT extends WorkflowEntity<WorkflowState> {
    @Override
    public Workflow<WorkflowState> definition() {
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

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class WorkflowEntityWithAcl extends WorkflowEntity<WorkflowState> {

    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowEntityWithMethodLevelAcl extends WorkflowEntity<WorkflowState> {

    @Override
    public Workflow<WorkflowState> definition() {
      return null;
    }

    @Acl(allow = @Acl.Matcher(service = "test"))
    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }
}
