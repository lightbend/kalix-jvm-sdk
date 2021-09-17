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

package com.akkaserverless.scalasdk.eventsourcedentity

import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntityProvider => Impl }
import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntity => EntityImpl }

//FIXME implement (the impl and type projection is temporary!)
abstract class EventSourcedEntity[S](val impl: EntityImpl[S]) {
  type Impl = EntityImpl[S]
}

//FIXME possibly The Provider will not delegate to javasdk and we'll duplicate some more code
class EventSourcedEntityProvider[S, E <: EventSourcedEntity[S]](private[akkaserverless] val impl: Impl[S, E#Impl])
