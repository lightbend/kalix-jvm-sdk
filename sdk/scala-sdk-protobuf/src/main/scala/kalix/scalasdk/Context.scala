/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import akka.stream.Materializer

trait Context {

  /**
   * An Akka Stream materializer to use for running streams. Needed for example in a command handler which accepts
   * streaming elements but returns a single async reply once all streamed elements has been consumed.
   */
  def materializer(): Materializer
}
