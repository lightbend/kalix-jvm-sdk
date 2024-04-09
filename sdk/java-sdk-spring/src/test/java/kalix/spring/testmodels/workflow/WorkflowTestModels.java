/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.workflow;

import kalix.javasdk.annotations.*;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class WorkflowTestModels {

  @TypeId("transfer-workflow")
  @Id("transferId")
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

  @TypeId("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithMethodLevelKey extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @Id("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithKeyOverridden extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @Id("transferId")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("id")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithIllDefinedIdGenerator extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @GenerateId
    @Id("id")
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithoutIdGeneratorAndId extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithIdGenerator extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @GenerateId
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("transferId")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithMethodLevelJWT extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    @JWT(
        validate = JWT.JwtMethodMode.BEARER_TOKEN,
        bearerTokenIssuer = {"a", "b"},
        staticClaims = {
            @JWT.StaticClaim(claim = "role", value = "method-admin"),
            @JWT.StaticClaim(claim = "aud", value = "${ENV}.kalix.io")
        })
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("transferId")
  @RequestMapping("/transfer/{transferId}")
  @JWT(
    validate = JWT.JwtMethodMode.BEARER_TOKEN,
    bearerTokenIssuer = {"c", "d"},
    staticClaims = {
        @JWT.StaticClaim(claim = "role", value = "admin"),
        @JWT.StaticClaim(claim = "aud", value = "${ENV}")
    })
  public static class WorkflowWithServiceLevelJWT extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("transferId")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class WorkflowWithAcl extends Workflow<WorkflowState> {

    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("transferId")
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
