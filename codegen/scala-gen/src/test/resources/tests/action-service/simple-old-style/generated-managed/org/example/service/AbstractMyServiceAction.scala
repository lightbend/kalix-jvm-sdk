package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import kalix.scalasdk.action.Action
import org.example.Components
import org.example.ComponentsImpl
import org.external.Empty

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractMyServiceAction extends Action {

  def components: Components =
    new ComponentsImpl(contextForComponents)

  def simpleMethod(myRequest: MyRequest): Action.Effect[Empty]

  def streamedOutputMethod(myRequest: MyRequest): Source[Action.Effect[Empty], NotUsed]

  def streamedInputMethod(myRequestSrc: Source[MyRequest, NotUsed]): Action.Effect[Empty]

  def fullStreamedMethod(myRequestSrc: Source[MyRequest, NotUsed]): Source[Action.Effect[Empty], NotUsed]
}

