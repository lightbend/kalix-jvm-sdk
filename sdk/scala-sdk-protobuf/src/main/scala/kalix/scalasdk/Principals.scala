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

package kalix.scalasdk

trait Principals {

  /**
   * Whether this request was from the internet.
   */
  def isInternet: Boolean

  /**
   * Whether this is a self request.
   */
  def isSelf: Boolean

  /**
   * Whether this request is a backoffice request.
   */
  def isBackoffice: Boolean

  /**
   * Whether this request was from a service in the local project.
   *
   * @param name
   *   The name of the service.
   */
  def isLocalService(name: String): Boolean

  /**
   * Whether this request was from any service in the local project.
   */
  def isAnyLocalService: Boolean

  /**
   * Get the service that invoked this call, if any.
   */
  def localService: Option[String]

  /**
   * Get the principals associated with this request.
   */
  def apply: Seq[Principal]

}
