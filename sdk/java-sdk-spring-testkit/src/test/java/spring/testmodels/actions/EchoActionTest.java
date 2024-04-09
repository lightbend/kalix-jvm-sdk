/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testmodels.actions;

import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.ActionTestkit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EchoActionTest {

  @Test
  public void testEchoCall() {
    ActionTestkit<EchoAction> actionUnitTestkit = ActionTestkit.of(EchoAction::new);
    ActionResult<String> result = actionUnitTestkit.call(echoAction -> echoAction.echo("Hey"));
    Assertions.assertEquals(result.getReply(), "Hey");
  }

  @Test
  public void testEchoCallWithMetadata() {
    ActionTestkit<EchoAction> actionUnitTestkit = ActionTestkit.of(EchoAction::new);
    ActionResult<String> result = actionUnitTestkit.call(echoAction -> echoAction.echoWithMetadata("Hey"), Metadata.EMPTY.add("key", "abc"));
    Assertions.assertEquals(result.getReply(), "abc");
  }
}
