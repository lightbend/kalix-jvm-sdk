package org.example.named.view

import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractMyUserByNameView extends View[UserState] {
  override def emptyState: UserState =
    null // emptyState is only used with transform_updates=true
}
