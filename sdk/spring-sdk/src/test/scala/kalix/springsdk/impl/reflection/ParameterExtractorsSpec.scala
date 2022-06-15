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

package kalix.springsdk.impl.reflection

import com.google.protobuf.ByteString
import com.google.protobuf.DynamicMessage
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.springsdk.impl.Introspector
import kalix.springsdk.impl.InvocationContext
import kalix.springsdk.impl.reflection.ParameterExtractors.BodyExtractor
import kalix.springsdk.testmodels.Message
import kalix.springsdk.testmodels.action.EchoAction
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParameterExtractorsSpec extends AnyWordSpec with Matchers {

  "BodyExtractor" should {

    "extract json payload from Any" in {
      val componentDescription = Introspector.inspect(classOf[EchoAction])
      val method = componentDescription.methods("MessageBody")

      val jsonBody = JsonSupport.encodeJson(new Message("test"))

      val field = method.messageDescriptor.findFieldByNumber(1)
      val message = DynamicMessage
        .newBuilder(method.messageDescriptor)
        .setField(field, jsonBody)
        .build()

      val wrappedMessage = ScalaPbAny().withValue(message.toByteString)

      val bodyExtractor: BodyExtractor[_] =
        method.parameterExtractors.collect { case extractor: BodyExtractor[_] => extractor }.head

      val context = InvocationContext(wrappedMessage, method.messageDescriptor)
      bodyExtractor.extract(context)

    }

    "reject non json payload" in {
      val componentDescription =
        Introspector.inspect(classOf[EchoAction])

      val method = componentDescription.methods("MessageBody")

      val nonJsonBody =
        JavaPbAny
          .newBuilder()
          .setTypeUrl("something.empty")
          .setValue(ByteString.EMPTY)
          .build()

      val field = method.messageDescriptor.findFieldByNumber(1)
      val message = DynamicMessage
        .newBuilder(method.messageDescriptor)
        .setField(field, nonJsonBody)
        .build()

      val wrappedMessage = ScalaPbAny().withValue(message.toByteString)

      val bodyExtractor: BodyExtractor[_] =
        method.parameterExtractors.collect { case extractor: BodyExtractor[_] => extractor }.head

      val context = InvocationContext(wrappedMessage, method.messageDescriptor)

      intercept[IllegalArgumentException] {
        bodyExtractor.extract(context)
      }

    }
  }

}
