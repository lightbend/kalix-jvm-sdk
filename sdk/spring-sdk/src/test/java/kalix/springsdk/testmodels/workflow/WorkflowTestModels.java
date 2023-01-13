package kalix.springsdk.testmodels.workflow;

import kalix.javasdk.workflow.Workflow;
import kalix.springsdk.annotations.Acl;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.GenerateEntityKey;
import kalix.springsdk.annotations.JWT;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class WorkflowTestModels {

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
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

  @EntityType("transfer-workflow")
  @RequestMapping("/transfer/{transferId}")
  public static class WorkflowWithMethodLevelKey extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
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
  public static class WorkflowWithKeyOverridden extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
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
  public static class WorkflowWithIllDefinedIdGenerator extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
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

  @EntityType("transfer-workflow")
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

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class WorkflowWithAcl extends Workflow<WorkflowState> {

    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }
  }

  @EntityType("transfer-workflow")
  @EntityKey("transferId")
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
