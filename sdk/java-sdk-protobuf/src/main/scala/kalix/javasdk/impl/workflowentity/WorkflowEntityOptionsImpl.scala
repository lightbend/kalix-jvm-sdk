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

package kalix.javasdk.impl.workflowentity

import java.util
import java.util.Collections

import kalix.javasdk.workflowentity.WorkflowEntityOptions

case class WorkflowEntityOptionsImpl(override val forwardHeaders: java.util.Set[String]) extends WorkflowEntityOptions {

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the incoming commands. By
   * default, no headers except "X-Server-Timing" are forwarded.
   */
  override def withForwardHeaders(headers: util.Set[String]): WorkflowEntityOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)))

}

object WorkflowEntityOptionsImpl {
  val defaults = new WorkflowEntityOptionsImpl(Collections.emptySet())
}
