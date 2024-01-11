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

package kalix.javasdk.impl

import akka.pattern.StatusReply
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
