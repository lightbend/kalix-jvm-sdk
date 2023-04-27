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

package kalix.scalasdk.testkit

import akka.annotation.{ ApiMayChange, InternalApi }
import com.google.protobuf.ByteString
import kalix.javasdk.testkit.{ EventingTestKit => JEventingTestKit }
import kalix.scalasdk.Metadata
import kalix.scalasdk.testkit.impl.TopicImpl

//FIXME add docs
@ApiMayChange
trait Topic {

  def expectNext(): Message[ByteString]

  def expectAll(): Seq[Message[ByteString]]
}

@InternalApi
private[testkit] object Topic {
  def apply(delegate: JEventingTestKit.Topic): Topic = TopicImpl(delegate)
}

@ApiMayChange
case class Message[P](payload: P, metadata: Metadata)
