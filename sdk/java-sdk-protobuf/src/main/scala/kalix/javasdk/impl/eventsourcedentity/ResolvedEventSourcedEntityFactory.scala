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

package kalix.javasdk.impl.eventsourcedentity

import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.javasdk.impl.EventSourcedEntityFactory
import kalix.javasdk.impl.ResolvedEntityFactory
import kalix.javasdk.impl.ResolvedServiceMethod

class ResolvedEventSourcedEntityFactory(
    delegate: EventSourcedEntityFactory,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]])
    extends EventSourcedEntityFactory
    with ResolvedEntityFactory {

  override def create(context: EventSourcedEntityContext): EventSourcedEntityRouter[_, _, _] =
    delegate.create(context)

}
