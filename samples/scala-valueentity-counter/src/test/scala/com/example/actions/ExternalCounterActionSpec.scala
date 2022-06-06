package com.example.actions

import com.example.{CounterService, IncreaseValue}
import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.MockRegistry
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ExternalCounterActionSpec
    extends AsyncWordSpec
    with Matchers
    with AsyncMockFactory {

  "ExternalCounterAction" must {

    "handle command Increase" in {

      val mockCounter = mock[CounterService]
      (mockCounter.increase _)
        .expects(*)
        .returning(Future.successful(Empty()))
      val mockRegistry = MockRegistry.withMock(mockCounter)

      val service = ExternalCounterActionTestKit(new ExternalCounterAction(_), mockRegistry)

      val result = service.increase(IncreaseValue()).asyncResult
      result.map { _ => succeed }
    }

  }
}
