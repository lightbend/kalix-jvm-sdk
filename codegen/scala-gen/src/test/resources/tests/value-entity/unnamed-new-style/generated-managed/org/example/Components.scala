package org.example

import com.akkaserverless.scalasdk.DeferredCall


// This code is managed by Akka Serverless tooling.
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
   def increase(command: _root_.org.example.valueentity.counter_api.IncreaseValue): DeferredCall[_root_.org.example.valueentity.counter_api.IncreaseValue, _root_.com.google.protobuf.empty.Empty]

   def decrease(command: _root_.org.example.valueentity.counter_api.DecreaseValue): DeferredCall[_root_.org.example.valueentity.counter_api.DecreaseValue, _root_.com.google.protobuf.empty.Empty]

 }

}
