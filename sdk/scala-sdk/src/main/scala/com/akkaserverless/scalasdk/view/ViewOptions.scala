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

package com.akkaserverless.scalasdk.view

import com.akkaserverless.javasdk.impl.ComponentOptions

import java.util
import scala.collection.immutable
import scala.jdk.CollectionConverters._

object ViewOptions {

  def defaults: ViewOptions = ViewOptionsImpl(Set.empty)

  private[akkaserverless] final case class ViewOptionsImpl(_forwardHeaders: Set[String]) extends ViewOptions {

    override def forwardHeaders(): java.util.Set[String] = _forwardHeaders.asJava

    /**
     * Ask Akka Serverless to forward these headers from the incoming request as metadata headers for the incoming
     * commands. By default no headers except "X-Server-Timing" are forwarded.
     */
    override def withForwardHeaders(headers: util.Set[String]): ComponentOptions =
      copy(_forwardHeaders = immutable.Set.from(headers.asScala))
  }
}
trait ViewOptions extends ComponentOptions
