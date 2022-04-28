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

 def myServiceNamedAction: MyServiceNamedActionCalls

}

object Components{

 trait MyServiceNamedActionCalls {
   def simpleMethod(command: _root_.org.example.service.MyRequest): DeferredCall[_root_.org.example.service.MyRequest, _root_.com.google.protobuf.empty.Empty]

 }

}
