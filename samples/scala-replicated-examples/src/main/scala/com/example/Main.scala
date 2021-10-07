package com.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.example.replicated.counter.Counter
import com.example.replicated.counter.CounterProvider
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("com.example.Main")

  def createAkkaServerless(): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless.register(CounterProvider(new Counter(_)))
    akkaServerless
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
