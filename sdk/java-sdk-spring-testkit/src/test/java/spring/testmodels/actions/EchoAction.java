/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testmodels.actions;

import kalix.javasdk.action.Action;

public class EchoAction extends Action {

  public Effect<String> echo(String msg) {
    return effects().reply(msg);
  }

  public Effect<String> echoWithMetadata(String msg) {
    return effects().reply(actionContext().metadata().get("key").get());
  }
}
