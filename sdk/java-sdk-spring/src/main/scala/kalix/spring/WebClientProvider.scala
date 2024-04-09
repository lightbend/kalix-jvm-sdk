/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring

import org.springframework.web.reactive.function.client.WebClient

trait WebClientProvider {

  /**
   * Returns a [[WebClient]] configured to connect to another Kalix service deployed on the same Kalix project.
   *
   * The service is identified only by the name it has been deployed. Kalix takes care of routing requests to the
   * service and keeping the data safe by encrypting the connection.
   */
  def webClientFor(serviceName: String): WebClient

}
