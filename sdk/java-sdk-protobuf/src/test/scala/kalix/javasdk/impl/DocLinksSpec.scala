package kalix.javasdk.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.Future

class DocLinksSpec extends AnyWordSpec with Matchers {

  "DocLinks" should {

    "specific error codes should be mapped to sdk specific urls" in {
      val javaPbDocLinks = new DocLinks("kalix-java-sdk-protobuf")
      javaPbDocLinks.forErrorCode("KLX-00112") shouldBe Some("https://docs.kalix.io/java-protobuf/views.html#changing")
      javaPbDocLinks.forErrorCode("KLX-00402") shouldBe Some("https://docs.kalix.io/java-protobuf/publishing-subscribing.html")
      javaPbDocLinks.forErrorCode("KLX-00415") shouldBe Some("https://docs.kalix.io/java-protobuf/publishing-subscribing.html#_subscribing_to_state_changes_from_a_value_entity")

      val javaDocLinks = new DocLinks("kalix-java-sdk-spring")
      javaDocLinks.forErrorCode("KLX-00112") shouldBe Some("https://docs.kalix.io/java/views.html#changing")
      javaDocLinks.forErrorCode("KLX-00402") shouldBe Some("https://docs.kalix.io/java/publishing-subscribing.html")
      javaDocLinks.forErrorCode("KLX-00415") shouldBe Some("https://docs.kalix.io/java/publishing-subscribing.html#_subscribing_to_state_changes_from_a_value_entity")
    }

    "fallback to general codes when no code matches" in {
      val javaDocLinks = new DocLinks("kalix-java-sdk-spring")

      javaDocLinks.forErrorCode("KLX-00100") shouldBe Some("https://docs.kalix.io/java/views.html")
      javaDocLinks.forErrorCode("KLX-00200") shouldBe Some("https://docs.kalix.io/java/value-entity.html")
    }

  }
}
