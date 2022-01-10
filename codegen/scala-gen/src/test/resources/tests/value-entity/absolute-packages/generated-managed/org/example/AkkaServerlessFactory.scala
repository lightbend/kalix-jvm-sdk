package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import org.example.domain.Counter
import org.example.domain.CounterProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createCounter: ValueEntityContext => Counter): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(CounterProvider(createCounter))
  }
}
