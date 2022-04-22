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

 def myUserByNameView: MyUserByNameViewCalls

}

object Components{

 trait MyUserByNameViewCalls {
   def getUserByName(command: _root_.org.example.named.view.ByNameRequest): DeferredCall[_root_.org.example.named.view.ByNameRequest, _root_.org.example.named.view.UserResponse]

 }

}
