package com.example

import kalix.scalasdk.Kalix
import com.example.actions.{CounterStateSubscriptionAction, DoubleCounterAction, ExternalCounterAction}
import com.example.domain.Counter
import org.slf4j.LoggerFactory

// tag::registration[]
// tag::registration-value-entity[]
object Main {

  private val log = LoggerFactory.getLogger("com.example.Main")

  def createKalix(): Kalix = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    // end::registration-value-entity[]
    // end::registration[]
    KalixFactory.withComponents(
      new Counter(_),
      new CounterStateSubscriptionAction(_),
      new DoubleCounterAction(_),
      new ExternalCounterAction(_))

    /* the comment hack bellow is needed to only show the Counter and DoubleCounterAction
    // tag::registration[]
    KalixFactory.withComponents(
      new Counter(_),
      new DoubleCounterAction(_))
    // end::registration[]
     */

    /* the comment hack bellow is needed to only show the Counter
    // tag::registration-value-entity[]
    KalixFactory.withComponents(
      new Counter(_))
    // end::registration-value-entity[]
     */
    // tag::registration-value-entity[]
    // tag::registration[]
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
// end::registration-value-entity[]
// end::registration[]
