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

import io.opentelemetry.context.propagation.TextMapGetter
import kalix.javasdk.Metadata

object TraceContextImpl {

  val getter: TextMapGetter[Metadata] = new TextMapGetter[Metadata]() {
    override def get(carrier: Metadata, key: String): String = {
      if (carrier.get(key).isPresent) {
        return carrier.get(key).get
      }
      null
    }

    override def keys(carrier: Metadata): java.lang.Iterable[String] = carrier.getAllKeys
  }
}
