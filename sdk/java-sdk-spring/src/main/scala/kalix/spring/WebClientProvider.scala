/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
