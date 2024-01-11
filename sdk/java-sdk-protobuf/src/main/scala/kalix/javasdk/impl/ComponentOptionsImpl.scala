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

import java.util
import java.util.Collections

private[impl] final case class ComponentOptionsImpl(override val forwardHeaders: java.util.Set[String])
    extends ComponentOptions {

  override def withForwardHeaders(headers: util.Set[String]): ComponentOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)));
}
