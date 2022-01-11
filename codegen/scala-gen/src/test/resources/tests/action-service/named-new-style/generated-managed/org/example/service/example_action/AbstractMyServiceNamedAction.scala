package org.example.service.example_action

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.akkaserverless.scalasdk.action.Action
import com.google.protobuf.empty.Empty
import org.example.Components
import org.example.ComponentsImpl

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An action. */
abstract class AbstractMyServiceNamedAction extends Action {

  def components: Components =
    new ComponentsImpl(actionContext)

  /** Handler for "simpleMethod". */
  def simpleMethod(myRequest: MyRequest): Action.Effect[Empty]

  /** Handler for "streamedOutputMethod". */
  def streamedOutputMethod(myRequest: MyRequest): Source[Action.Effect[Empty], NotUsed]

  /** Handler for "streamedInputMethod". */
  def streamedInputMethod(myRequestSrc: Source[MyRequest, NotUsed]): Action.Effect[Empty]

  /** Handler for "fullStreamedMethod". */
  def fullStreamedMethod(myRequestSrc: Source[MyRequest, NotUsed]): Source[Action.Effect[Empty], NotUsed]
}

