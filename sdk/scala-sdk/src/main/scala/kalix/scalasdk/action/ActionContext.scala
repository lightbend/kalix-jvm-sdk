/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.scalasdk.action

import kalix.scalasdk.Metadata
import kalix.scalasdk.MetadataContext

trait ActionContext extends MetadataContext {

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
   * The origin subject of the {{{CloudEvent}}}. For example, the entity key when the event was emitted from an entity.
   */
  def eventSubject: Option[String]

  /**
   * Get an Akka gRPC client for the given service name. The same client instance is shared across components in the
   * application. The lifecycle of the client is managed by the SDK and it should not be stopped by user code.
   *
   * @tparam T
   *   The "service" interface generated for the service by Akka gRPC
   * @param clientClass
   *   The class of a gRPC service generated by Akka gRPC
   * @param service
   *   The name of the service to connect to, either a name of another Kalix service or an external service
   *   where connection details are configured under `akka.grpc.client.[service-name]` in `application.conf`.
   */
  def getGrpcClient[T](clientClass: Class[T], service: String): T
}
