/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.action

import kalix.scalasdk.action.Action

class ActionTwoImpl extends AbstractActionTwoAction {

  override def call(otherRequest: OtherRequest): Action.Effect[Response] =
    effects.reply(Response.defaultInstance)
}
