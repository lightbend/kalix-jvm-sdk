/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import io.grpc.StatusRuntimeException
import kalix.javasdk.DeferredCall
import kalix.javasdk.Metadata

import java.util.concurrent.CompletionStage

/**
 * INTERNAL API
 */
final case class GrpcDeferredCall[I, O](
    message: I,
    metadata: MetadataImpl,
    fullServiceName: String,
    methodName: String,
    asyncCall: Metadata => CompletionStage[O])
    extends DeferredCall[I, O] {
  override def execute(): CompletionStage[O] = asyncCall(metadata).exceptionally {
    case sre: StatusRuntimeException =>
      throw new StatusRuntimeException(
        sre.getStatus
          .augmentDescription(s"When calling Kalix service $fullServiceName/$methodName")
          .withCause(sre.getCause))
    case other: Throwable => throw other
  }

  override def withMetadata(metadata: Metadata): GrpcDeferredCall[I, O] = {
    this.copy(metadata = metadata.asInstanceOf[MetadataImpl])
  }
}
