/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
