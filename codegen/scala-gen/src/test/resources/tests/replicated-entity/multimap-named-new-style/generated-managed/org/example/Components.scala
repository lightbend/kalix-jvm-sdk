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

 def someMultiMap: SomeMultiMapCalls

}

object Components{

 trait SomeMultiMapCalls {
   def put(command: _root_.com.example.replicated.multimap.PutValue): DeferredCall[_root_.com.example.replicated.multimap.PutValue, _root_.com.google.protobuf.empty.Empty]

 }

}
