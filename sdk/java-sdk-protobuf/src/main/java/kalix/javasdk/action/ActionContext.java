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

package kalix.javasdk.action;

import kalix.javasdk.CloudEvent;
import kalix.javasdk.Metadata;
import kalix.javasdk.MetadataContext;

import java.util.Optional;

/** Context for action calls. */
public interface ActionContext extends MetadataContext, ActionCreationContext {

  // FIXME: remove this method and move docs currently pointing
  // to this method toMetadataContext
  /**
   * Get the metadata associated with this call.
   *
   * <p>Note, this only returns call level associated metadata. For unary calls, this will be the
   * same as the message metadata, but for streamed calls, it will contain metadata associated with
   * the whole stream, so for example if this was a gRPC call, it will contain the HTTP headers for
   * that gRPC call.
   *
   * @return The call level metadata.
   */
  Metadata metadata();

  /**
   * The origin subject of the {@link CloudEvent}. For example, the entity id when the event was
   * emitted from an entity.
   */
  Optional<String> eventSubject();
}
