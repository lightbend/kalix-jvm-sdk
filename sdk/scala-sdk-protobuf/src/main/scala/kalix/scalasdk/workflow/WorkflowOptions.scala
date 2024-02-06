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

package kalix.scalasdk.workflow

import kalix.scalasdk.impl.ComponentOptions

/** Workflow options. */
trait WorkflowOptions extends ComponentOptions {

  def withForwardHeaders(headers: Set[String]): WorkflowOptions
}
object WorkflowOptions {
  val defaults: WorkflowOptions =
    WorkflowOptionsImpl(Set.empty)

  private[kalix] final case class WorkflowOptionsImpl(forwardHeaders: Set[String]) extends WorkflowOptions {

    override def withForwardHeaders(headers: Set[String]): WorkflowOptionsImpl =
      copy(forwardHeaders = headers)
  }
}
