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

package com.akkaserverless.scalasdk.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntityProvider => Impl }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedData => DataImpl }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntity => EntityImpl }

//FIXME implement (the impl and type projection is temporary!)
trait ReplicatedData {
  type Impl = DataImpl
}

//FIXME implement (the impl and type projection is temporary!)
abstract class ReplicatedEntity[D <: ReplicatedData](val impl: EntityImpl[D#Impl]) {
  type Impl = EntityImpl[D#Impl]
}

//FIXME possibly The Provider will not delegate to javasdk and we'll duplicate some more code
class ReplicatedEntityProvider[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    private[akkaserverless] val impl: Impl[D#Impl, E#Impl])
