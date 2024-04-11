/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.javasdk.StatusCode.Redirect
import kalix.javasdk.StatusCode.Success

import java.time.Instant
import java.util.Optional
import kalix.javasdk.{ Metadata, Principal }
import kalix.protocol.component.MetadataEntry
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._

class MetadataImplSpec extends AnyWordSpec with Matchers with OptionValues {

  "MetadataImpl" should {
    "support getting the subject JWT claim" in {
      metadata("_kalix-jwt-claim-sub" -> "some-subject").jwtClaims.subject().toScala.value shouldBe "some-subject"
    }

    "support getting the expiration JWT claim" in {
      metadata("_kalix-jwt-claim-exp" -> "12345").jwtClaims.expirationTime().toScala.value shouldBe Instant
        .ofEpochSecond(12345)
    }

    "support parsing object JWT claims" in {
      val jsonNode =
        metadata("_kalix-jwt-claim-my-object" -> """{"foo":"bar"}""").jwtClaims().getObject("my-object").toScala.value
      jsonNode.get("foo").textValue() shouldBe "bar"
    }

    "support parsing string list JWT claims" in {
      val list = metadata("_kalix-jwt-claim-my-string-list" -> """["foo","bar"]""")
        .jwtClaims()
        .getStringList("my-string-list")
        .toScala
        .value
      (list.asScala should contain).theSameElementsInOrderAs(List("foo", "bar"))
    }

    "support parsing int list JWT claims" in {
      val list = metadata("_kalix-jwt-claim-my-int-list" -> """[3,4]""")
        .jwtClaims()
        .getIntegerList("my-int-list")
        .toScala
        .value
      (list.asScala should contain).theSameElementsInOrderAs(List(3, 4))
    }

    "ignore claims that are not the right type" in {
      val meta = metadata("_kalix-jwt-claim-foo" -> "bar")
      meta.jwtClaims().getBoolean("foo").toScala shouldBe None
      meta.jwtClaims().getInteger("foo").toScala shouldBe None
      meta.jwtClaims().getLong("foo").toScala shouldBe None
      meta.jwtClaims().getDouble("foo").toScala shouldBe None
      meta.jwtClaims().getNumericDate("foo").toScala shouldBe None
      meta.jwtClaims().getObject("foo").toScala shouldBe None
      meta.jwtClaims().getBooleanList("foo").toScala shouldBe None
      meta.jwtClaims().getIntegerList("foo").toScala shouldBe None
      meta.jwtClaims().getLongList("foo").toScala shouldBe None
      meta.jwtClaims().getDoubleList("foo").toScala shouldBe None
      meta.jwtClaims().getNumericDateList("foo").toScala shouldBe None
      meta.jwtClaims().getObjectList("foo").toScala shouldBe None
      meta.jwtClaims().getStringList("foo").toScala shouldBe None
    }

    "ignore claims that don't exist" in {
      val meta = metadata("_kalix-jwt-claim-x" -> "bar")
      meta.jwtClaims().getString("foo").toScala shouldBe None
      meta.jwtClaims().getBoolean("foo").toScala shouldBe None
      meta.jwtClaims().getInteger("foo").toScala shouldBe None
      meta.jwtClaims().getLong("foo").toScala shouldBe None
      meta.jwtClaims().getDouble("foo").toScala shouldBe None
      meta.jwtClaims().getNumericDate("foo").toScala shouldBe None
      meta.jwtClaims().getObject("foo").toScala shouldBe None
      meta.jwtClaims().getBooleanList("foo").toScala shouldBe None
      meta.jwtClaims().getIntegerList("foo").toScala shouldBe None
      meta.jwtClaims().getLongList("foo").toScala shouldBe None
      meta.jwtClaims().getDoubleList("foo").toScala shouldBe None
      meta.jwtClaims().getNumericDateList("foo").toScala shouldBe None
      meta.jwtClaims().getObjectList("foo").toScala shouldBe None
      meta.jwtClaims().getStringList("foo").toScala shouldBe None
    }

    "support accessing principals" when {
      "the principal is the internet" in {
        val meta = metadata("_kalix-src" -> "internet")
        meta.principals().isInternet shouldBe true
        meta.principals().get().asScala should contain only Principal.INTERNET
      }
      "the principal is not the internet" in {
        metadata("_kalix-src" -> "self").principals().isInternet shouldBe false
        metadata("_kalix-src-svc" -> "foo").principals().isInternet shouldBe false
      }
      "the principal is self" in {
        val meta = metadata("_kalix-src" -> "self")
        meta.principals().isSelf shouldBe true
        meta.principals().get().asScala should contain only Principal.SELF
      }
      "the principal is not self" in {
        metadata("_kalix-src" -> "internet").principals().isSelf shouldBe false
        metadata("_kalix-src-svc" -> "foo").principals().isSelf shouldBe false
      }
      "the principal is the backoffice" in {
        val meta = metadata("_kalix-src" -> "backoffice")
        meta.principals().isBackoffice shouldBe true
        meta.principals().get().asScala should contain only Principal.BACKOFFICE
      }
      "the principal is not backoffice" in {
        metadata("_kalix-src" -> "internet").principals().isBackoffice shouldBe false
        metadata("_kalix-src-svc" -> "foo").principals().isBackoffice shouldBe false
      }
      "the principal is a local service" in {
        val meta = metadata("_kalix-src-svc" -> "foo")
        meta.principals().isLocalService("foo") shouldBe true
        meta.principals().isLocalService("bar") shouldBe false
        meta.principals().getLocalService shouldBe Optional.of("foo")
        meta.principals().isAnyLocalService shouldBe true
        meta.principals().get().asScala should contain only Principal.localService("foo")
      }
      "the principal is not a local service" in {
        metadata("_kalix-src" -> "internet").principals().isAnyLocalService shouldBe false
        metadata("_kalix-src" -> "internet").principals().isLocalService("foo") shouldBe false
      }
      "the principal is unrecognised" in {
        val meta = metadata("_kalix-src" -> "blah")
        meta.principals().isInternet shouldBe false
        meta.principals().isAnyLocalService shouldBe false
        meta.principals().get().asScala should have size 0
      }
    }

    "support setting a HTTP status code" in {
      val md = Metadata.EMPTY.withStatusCode(Success.CREATED)
      md.get("_kalix-http-code").toScala.value shouldBe "201"

      val mdRedirect = md.withStatusCode(Redirect.MOVED_PERMANENTLY)
      mdRedirect.get("_kalix-http-code").toScala.value shouldBe "301"
    }

    "support creationg with CloudEvents prefixed with ce_" in {
      val md = metadata("ce_id" -> "id", "ce_source" -> "source", "ce_specversion" -> "1.0", "ce_type" -> "foo")
      md.isCloudEvent shouldBe true
      val ce = md.asCloudEvent()
      ce.id() shouldBe "id"
      ce.source().toString shouldBe "source"
      ce.specversion() shouldBe "1.0"
      ce.`type`() shouldBe "foo"
    }
  }

  private def metadata(entries: (String, String)*): Metadata = {
    MetadataImpl.of(entries.map { case (key, value) =>
      MetadataEntry(key, MetadataEntry.Value.StringValue(value))
    })
  }

}
