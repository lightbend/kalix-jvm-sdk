/*
 * Copyright 2024 Lightbend Inc.
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
