package com.example

import spray.json.{JsValue, enrichAny}

import java.util.Base64
import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

// tag::test-jwt[]
class JwtIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  
  // tag::jwt-in-header[]
  private val client = testKit.getGrpcClient(classOf[JwtServiceAction])
    .asInstanceOf[JwtServiceActionClient] // <1>

  // end::jwt-in-header[]

  // tag::jwt-in-message[]
  private val basicClient = testKit.getGrpcClient(classOf[JwtServiceAction])

  // tag::jwt-in-header[]
  "JwtServiceAction" must {

    // end::jwt-in-message[]

    "accept requests with a valid bearer token passed as metadata" in {
      val token = bearerTokenWith(Map("iss" -> "my-issuer", "sub"-> "hello")) // <2>
      val msg = "hello from integration test"
      val response = client.jwtInToken()
        .addHeader("Authorization", "Bearer " + token) // <3>
        .invoke(MyRequest(msg))

      response.futureValue.msg should include(msg)
    }
    // end::jwt-in-header[]

    // tag::jwt-in-message[]
    "accept requests with a valid bearer token inside the request message" in {
      val token = bearerTokenWith(Map("iss" -> "my-issuer", "sub"-> "hello"))
      val msg = "hello from integration test"
      val response = basicClient.jwtInMessage(MyRequestWithToken(msg, token))

      response.futureValue.msg should include(msg)
    }

  // tag::jwt-in-header[]
  }
  // tag::jwt-in-message[]
  // end::jwt-in-header[]


  // tag::jwt-util[]

  private def bearerTokenWith(claims: Map[String, String]): String = {
    // setting algorithm to none
    val alg = Base64.getEncoder.encodeToString("""{"alg":"none"}""".getBytes); // <4>

    import spray.json.DefaultJsonProtocol._
    val claimsJson: JsValue = claims.toJson

    // no validation is done for integration tests, thus no valid signature required
    s"$alg.${Base64.getEncoder.encodeToString(claimsJson.toString().getBytes)}" // <5>
  }
  // end::jwt-util[]

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
// end::test-topic[]
