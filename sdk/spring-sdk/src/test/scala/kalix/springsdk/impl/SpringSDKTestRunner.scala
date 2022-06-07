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

package kalix.springsdk.impl

import java.util.function

import akka.actor.ActorSystem
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.{ any, ByteString }
import kalix.javasdk.{ JsonSupport, KalixRunner }
import kalix.javasdk.impl.{ AnySupport, MessageInDecoder, MessageOutEncoder, Service }
import kalix.javasdk.impl.action.ActionService
import kalix.springsdk.action.EchoAction
import kalix.springsdk.impl.action.{ ActionIntrospector, ActionReflectiveRouter }
import kalix.springsdk.impl.reflection.NameGenerator

import scala.jdk.CollectionConverters._

// Temporary class just for testing
object SpringSDKTestRunner extends App {

  val objectMapper = new ObjectMapper()
  val nameGenerator = new NameGenerator
  val factory = ActionIntrospector.inspect(classOf[EchoAction], nameGenerator, objectMapper, () => new EchoAction)
  val anySupport = new AnySupport(Array(factory.fileDescriptor), getClass.getClassLoader)

  val service = new ActionService(
    factory,
    factory.serviceDescriptor,
    ProtoDescriptorGenerator.dependencies,
    anySupport,
    new MessageInDecoder {
      override def decodeMessage(value: any.Any): Any = value
    },
    new MessageOutEncoder {
      override def encodeScala(value: Any): any.Any = {
        any.Any.fromJavaProto(JsonSupport.encodeJson(value))
      }
    })

  val runner = new KalixRunner(
    Map(factory.serviceDescriptor.getFullName -> new function.Function[ActorSystem, Service] {
      override def apply(system: ActorSystem): Service = service
    }).asJava)
  runner.run().toCompletableFuture.get()
}
