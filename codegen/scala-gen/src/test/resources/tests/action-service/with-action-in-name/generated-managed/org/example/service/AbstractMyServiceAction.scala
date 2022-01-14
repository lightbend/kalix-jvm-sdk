package org.example.service

import com.akkaserverless.scalasdk.action.Action
import com.google.protobuf.empty.Empty
import org.example.Components
import org.example.ComponentsImpl

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An action. */
abstract class AbstractMyServiceAction extends Action {

  def components: Components =
    new ComponentsImpl(actionContext)

  /** Handler for "simpleMethod". */
  def simpleMethod(myRequest: MyRequest): Action.Effect[Empty]
}

