package org.example.named.view

import kalix.javasdk.impl.view.UpdateHandlerNotFound
import kalix.scalasdk.impl.view.ViewRouter
import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

class MyUserByNameViewRouter(view: MyUserByNameView)
  extends ViewRouter[MyUserByNameView](view) {

  override def handleUpdate[S](
      eventName: String,
      state: S,
      event: Any): View.UpdateEffect[S] = {

    eventName match {
      

      case _ =>
        throw new UpdateHandlerNotFound(eventName)
    }
  }

}
