package org.example

import kalix.scalasdk.DeferredCall


// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for user extension, provided through generated implementation
 */
trait Components {
 import Components._

 def someServiceAction: SomeServiceActionCalls
 def transferWorkflow: TransferWorkflowCalls

}

object Components{

 trait SomeServiceActionCalls {
   def simpleMethod(command: _root_.org.example.service.SomeRequest): DeferredCall[_root_.org.example.service.SomeRequest, _root_.com.google.protobuf.empty.Empty]

 }
 trait TransferWorkflowCalls {
   def start(command: _root_.org.example.workflow.Transfer): DeferredCall[_root_.org.example.workflow.Transfer, _root_.com.google.protobuf.empty.Empty]

   def getState(command: _root_.com.google.protobuf.empty.Empty): DeferredCall[_root_.com.google.protobuf.empty.Empty, _root_.org.example.workflow.Transfer]

 }

}
