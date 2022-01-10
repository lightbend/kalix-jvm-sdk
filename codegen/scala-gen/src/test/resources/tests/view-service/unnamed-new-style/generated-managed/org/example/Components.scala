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

 def userByNameView: UserByNameViewCalls

}

object Components{

 trait UserByNameViewCalls {
   def getUserByName(command: _root_.org.example.unnamed.view.example_unnamed_views.ByNameRequest): DeferredCall[_root_.org.example.unnamed.view.example_unnamed_views.ByNameRequest, _root_.org.example.unnamed.view.example_unnamed_views.UserResponse]

 }

}
