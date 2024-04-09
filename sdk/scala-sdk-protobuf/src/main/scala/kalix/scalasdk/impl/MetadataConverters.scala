/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl

import kalix.javasdk
import kalix.scalasdk

private[scalasdk] object MetadataConverters {
  def toScala(javaSdkMetadata: javasdk.Metadata): scalasdk.Metadata =
    // FIXME can we get rid of this cast?
    new scalasdk.impl.MetadataImpl(javaSdkMetadata.asInstanceOf[javasdk.impl.MetadataImpl])

  def toJava(scalaSdkMetadata: scalasdk.Metadata): javasdk.Metadata =
    scalaSdkMetadata.impl

}
