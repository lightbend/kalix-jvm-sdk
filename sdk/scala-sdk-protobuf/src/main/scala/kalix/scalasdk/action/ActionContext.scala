/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.action

import kalix.scalasdk.{ Metadata, MetadataContext }

trait ActionContext extends MetadataContext with ActionCreationContext {

  /**
   * Get the metadata associated with this call.
   *
   * <p>Note, this only returns call level associated metadata. For unary calls, this will be the same as the message
   * metadata, but for streamed calls, it will contain metadata associated with the whole stream, so for example if this
   * was a gRPC call, it will contain the HTTP headers for that gRPC call.
   *
   * @return
   *   The call level metadata.
   */
  def metadata: Metadata

  /**
   * The origin subject of the {{{CloudEvent}}}. For example, the entity id when the event was emitted from an entity.
   */
  def eventSubject: Option[String]
}
