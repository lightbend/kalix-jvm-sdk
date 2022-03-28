package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import org.external.Empty

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class MyServiceAction(creationContext: ActionCreationContext) extends AbstractMyServiceAction {

  override def simpleMethod(myRequest: MyRequest): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `simpleMethod` is not implemented, yet")
  }

  override def streamedOutputMethod(myRequest: MyRequest): Source[Action.Effect[Empty], NotUsed] = {
    throw new RuntimeException("The command handler for `streamedOutputMethod` is not implemented, yet")
  }

  override def streamedInputMethod(myRequestSrc: Source[MyRequest, NotUsed]): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `streamedInputMethod` is not implemented, yet")
  }

  override def fullStreamedMethod(myRequestSrc: Source[MyRequest, NotUsed]): Source[Action.Effect[Empty], NotUsed] = {
    throw new RuntimeException("The command handler for `fullStreamedMethod` is not implemented, yet")
  }
}

