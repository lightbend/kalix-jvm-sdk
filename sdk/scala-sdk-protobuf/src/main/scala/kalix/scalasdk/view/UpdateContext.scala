/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.view

import kalix.scalasdk.MetadataContext

/** Context for view update calls. */
trait UpdateContext extends ViewContext with MetadataContext {

  /**
   * The origin subject of the [[kalix.scalasdk.CloudEvent]]. For example, the entity id when the event was emitted from
   * an entity.
   */
  def eventSubject: Option[String]

  /** The name of the event being handled. */
  def eventName: String
}
