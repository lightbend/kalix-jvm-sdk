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

 def counterServiceEntity: CounterServiceEntityCalls

}

object Components{

 trait CounterServiceEntityCalls {
   def increase(command: _root_.org.example.valueentity.IncreaseValue): DeferredCall[_root_.org.example.valueentity.IncreaseValue, _root_.com.google.protobuf.empty.Empty]

   def decrease(command: _root_.org.example.valueentity.DecreaseValue): DeferredCall[_root_.org.example.valueentity.DecreaseValue, _root_.com.google.protobuf.empty.Empty]

 }

}
