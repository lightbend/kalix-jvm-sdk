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

package com.akkaserverless.scalasdk.testkit.impl

import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.testkit.DeferredCallDetails

import scala.concurrent.Future

final case class TestKitDeferredCall[I, O](message: I, metadata: Metadata, serviceName: String, methodName: String)
    extends DeferredCallDetails[I, O] {

  def execute(): Future[O] =
    throw new UnsupportedOperationException("Async calls to other components not supported by the testkit")
}
