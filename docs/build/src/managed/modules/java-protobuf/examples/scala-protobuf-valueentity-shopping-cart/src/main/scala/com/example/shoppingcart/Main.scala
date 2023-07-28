package com.example.shoppingcart

import kalix.scalasdk.Kalix
import com.example.shoppingcart.domain.ShoppingCart
import com.example.shoppingcart.domain.ShoppingCartProvider
import kalix.scalasdk.action.ActionOptions
import kalix.scalasdk.valueentity.ValueEntityOptions
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::forward-headers[]
object Main {
  // end::forward-headers[]
  private val log = LoggerFactory.getLogger("com.example.shoppingcart.Main")
  // tag::forward-headers[]

  def createKalix(): Kalix = {
    val kalix = Kalix()
    val forwardHeaders = ActionOptions.defaults
      .withForwardHeaders(Set("UserRole")) // <1>
    kalix
      .register(ShoppingCartActionProvider(new ShoppingCartActionImpl(_))
        .withOptions(forwardHeaders)) // <2>
      // end::forward-headers[]
      .register(ShoppingCartProvider(new ShoppingCart(_))
        .withOptions(ValueEntityOptions.defaults.withForwardHeaders(Set("Role"))))
    // tag::forward-headers[]
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
// end::forward-headers[]
