package org.example.view

import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
import com.akkaserverless.scalasdk.impl.view.ViewRouter
import com.akkaserverless.scalasdk.view.View

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** A view handler */
class UserByNameViewRouter(view: UserByNameViewImpl)
  extends ViewRouter[UserState, UserByNameViewImpl](view) {

  override def handleUpdate(
      eventName: String,
      state: UserState,
      event: Any): View.UpdateEffect[UserState] = {

    eventName match {
      

      case _ =>
        throw new UpdateHandlerNotFound(eventName)
    }
  }

}
