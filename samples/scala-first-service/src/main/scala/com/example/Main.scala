package com.example

import kalix.scalasdk.AkkaServerless
import com.example.domain.Counter
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("com.example.Main")

  def createKalix(): AkkaServerless = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `AkkaServerless()` instance.
    KalixFactory.withComponents(
      new Counter(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
