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

 def counter: CounterCalls

}

object Components{

 trait CounterCalls {
   def increase(command: _root_.org.example.eventsourcedentity.IncreaseValue): DeferredCall[_root_.org.example.eventsourcedentity.IncreaseValue, _root_.com.google.protobuf.empty.Empty]

   def decrease(command: _root_.org.example.eventsourcedentity.DecreaseValue): DeferredCall[_root_.org.example.eventsourcedentity.DecreaseValue, _root_.com.google.protobuf.empty.Empty]

 }

}
