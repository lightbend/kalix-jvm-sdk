package com.example.actions;

import akka.Done;
import kalix.javasdk.action.Action;

// tag::class[]
public class SubscribeToRawBytesAction extends Action {

  public Effect<Done> onMessage(byte[] bytes) { // <1>
    // deserialization logic here
    return effects().reply(Done.done());
  }
}
// end::class[]