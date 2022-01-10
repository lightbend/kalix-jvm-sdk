package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import org.example.domain.Counter
import org.example.domain.CounterProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounter: EventSourcedEntityContext => Counter): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterProvider(createCounter))
  }
}
