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

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.CloudEvent;
import com.akkaserverless.javasdk.MetadataContext;

import java.util.Optional;

/** Context for view update calls. */
public interface UpdateHandlerContext extends ViewContext, MetadataContext {

  /**
   * The origin subject of the {@link CloudEvent}. For example, the entity key when the event was
   * emitted from an entity.
   */
  Optional<String> eventSubject();

  /** The name of the command being executed. */
  String commandName();
}
