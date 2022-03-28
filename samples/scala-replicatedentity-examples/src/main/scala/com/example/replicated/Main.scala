package com.example.replicated

import kalix.scalasdk.Kalix
import com.example.replicated.counter.domain.SomeCounter
import com.example.replicated.countermap.domain.SomeCounterMap
import com.example.replicated.map.domain.SomeMap
import com.example.replicated.multimap.domain.SomeMultiMap
import com.example.replicated.register.domain.SomeRegister
import com.example.replicated.registermap.domain.SomeRegisterMap
import com.example.replicated.set.domain.SomeSet
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("com.example.replicated.Main")

  def createKalix(): Kalix = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    KalixFactory.withComponents(
      new SomeCounter(_),
      new SomeCounterMap(_),
      new SomeMap(_),
      new SomeMultiMap(_),
      new SomeRegister(_),
      new SomeRegisterMap(_),
      new SomeSet(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
