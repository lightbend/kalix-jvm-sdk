/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

/** Context that provides access to metadata. */
trait MetadataContext extends Context {

  /** Get the metadata associated with this context. */
  def metadata: Metadata

}
