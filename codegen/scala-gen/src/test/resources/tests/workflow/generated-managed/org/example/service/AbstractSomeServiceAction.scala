package org.example.service

import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import org.example.Components
import org.example.ComponentsImpl

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractSomeServiceAction extends Action {

  def components: Components =
    new ComponentsImpl(contextForComponents)

  def simpleMethod(someRequest: SomeRequest): Action.Effect[Empty]
}

