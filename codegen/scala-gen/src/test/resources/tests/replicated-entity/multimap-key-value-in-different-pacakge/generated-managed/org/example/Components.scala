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

 def someMultiMap: SomeMultiMapCalls

}

object Components{

 trait SomeMultiMapCalls {
   def put(command: _root_.com.example.replicated.multimap.multi_map_api.PutValue): DeferredCall[_root_.com.example.replicated.multimap.multi_map_api.PutValue, _root_.com.google.protobuf.empty.Empty]

 }

}
