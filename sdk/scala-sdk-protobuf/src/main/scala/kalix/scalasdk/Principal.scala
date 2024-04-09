/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import kalix.javasdk.{ Principal => JPrincipal }

/**
 * A principal associated with a request.
 */
sealed trait Principal

object Principal {

  /** Abstract principal representing all requests from the internet */
  case object Internet extends Principal

  /** Abstract principal representing all requests from self */
  case object Self extends Principal

  /** Abstract principal representing all requests from the backoffice */
  case object Backoffice extends Principal

  /**
   * A local service principal.
   */
  case class LocalService(name: String) extends Principal

  private[scalasdk] def toJava(principal: Principal): JPrincipal = principal match {
    case Internet           => JPrincipal.INTERNET
    case Self               => JPrincipal.SELF
    case Backoffice         => JPrincipal.BACKOFFICE
    case LocalService(name) => JPrincipal.localService(name)
  }

  private[scalasdk] def toScala(principal: JPrincipal): Principal = principal match {
    case JPrincipal.INTERNET                   => Internet
    case JPrincipal.SELF                       => Self
    case JPrincipal.BACKOFFICE                 => Backoffice
    case localService: JPrincipal.LocalService => LocalService(localService.getName)
  }

}
