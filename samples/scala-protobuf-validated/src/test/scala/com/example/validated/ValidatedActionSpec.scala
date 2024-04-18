package com.example.validated

import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ValidatedActionSpec extends AnyWordSpec with Matchers {

  "ValidatedAction" must {

    "not allow construction of request with invalid email" in {
      intercept[scalapb.validate.FieldValidationException] {
        Request("invalid email")
      }
    }

  }
}
