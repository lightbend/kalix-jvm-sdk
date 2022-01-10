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

package com.akkaserverless.tck.model

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.tck.model.action.ActionTckModelImpl
import com.akkaserverless.tck.model.action.ActionTwoImpl
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedConfiguredEntity
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTckModelEntity
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTwoEntity
import com.akkaserverless.tck.model.valueentity.ValueEntityConfiguredEntity
import com.akkaserverless.tck.model.valueentity.ValueEntityTckModelEntity
import com.akkaserverless.tck.model.valueentity.ValueEntityTwoEntity
import com.akkaserverless.tck.model.view.ViewTckModelImpl
import com.akkaserverless.tck.model.view.ViewTckSourceEntity
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("com.akkaserverless.tck.model.Main")

  def createAkkaServerless(): AkkaServerless = {
    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `AkkaServerless()` instance.
    AkkaServerlessFactory.withComponents(
      new EventSourcedConfiguredEntity(_),
      new EventSourcedTckModelEntity(_),
      new EventSourcedTwoEntity(_),
      new ValueEntityConfiguredEntity(_),
      new ValueEntityTckModelEntity(_),
      new ValueEntityTwoEntity(_),
      new ViewTckSourceEntity(_),
      new ActionTckModelImpl(_),
      new ActionTwoImpl(_),
      new ViewTckModelImpl(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
