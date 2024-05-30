package com.example

import kalix.scalasdk.Kalix
import org.slf4j.LoggerFactory
import sttp.client4.httpclient.HttpClientFutureBackend

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("com.example.Main")

  val backend = HttpClientFutureBackend()
  def createKalix(): Kalix = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    KalixFactory.withComponents(new ControllerAction(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    def close: Runnable = () => backend.close()
    Runtime.getRuntime.addShutdownHook { new Thread(close) }
    createKalix().start()
  }
}
