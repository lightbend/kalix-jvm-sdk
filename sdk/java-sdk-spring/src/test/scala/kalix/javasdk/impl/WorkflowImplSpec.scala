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

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{Any => ScalaPbAny}
import kalix.javasdk.impl.workflow.TestWorkflow
import kalix.javasdk.workflow.ReflectiveWorkflowProvider
import kalix.javasdk.workflow.TestWorkflowSerialization
import kalix.javasdk.workflow.TestWorkflowSerialization.Result
import kalix.testkit.TestProtocol
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class WorkflowImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with Eventually {
  import kalix.testkit.workflow.WorkflowMessages._

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(10, Seconds), Span(100, Millis))

  "Workflow" should {

    "deserialize response" in {
      val entityId = "1"
      val jsonMessageCodec = new JsonMessageCodec()
      val service = new TestWorkflow(
        ReflectiveWorkflowProvider
          .of[String, TestWorkflowSerialization](
            classOf[TestWorkflowSerialization],
            jsonMessageCodec,
            _ => new TestWorkflowSerialization()))
      val protocol = TestProtocol(service.port)
      val workflow = protocol.workflow.connect()

      workflow.send(init(classOf[TestWorkflowSerialization].getName, entityId))

      workflow.expect(config())

      val emptyState = jsonMessageCodec.encodeScala("empty")
      val stepResult = jsonMessageCodec.encodeScala(new Result.Succeed())

      workflow.send(command(1, entityId, "Start", emptySyntheticRequest("Start")))
      workflow.expect(reply(1, jsonMessageCodec.encodeScala("ok"), emptyState, stepTransition("test")))

      workflow.send(executeStep(2, "test", emptyState))
      workflow.expect(stepExecuted(2, "test", stepResult))

      workflow.send(getNextStep(3, "test", stepResult))
      workflow.expect(end(3, jsonMessageCodec.encodeScala("success")))

      workflow.send(command(1, entityId, "Get", emptySyntheticRequest("Get")))
      workflow.expect(reply(1, jsonMessageCodec.encodeScala("success")))

      protocol.terminate()
      service.terminate()
    }
  }

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.workflow.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
