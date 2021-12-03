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

package com.akkaserverless.javasdk.impl

import java.time.Instant

import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.protocol.component.MetadataEntry
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._

class MetadataImplSpec extends AnyWordSpec with Matchers with OptionValues {

  "MetadataImpl" should {
    "support getting the subject JWT claim" in {
      metadata("_akkasls-jwt-claim-sub" -> "some-subject").jwtClaims.subject().toScala.value shouldBe "some-subject"
    }

    "support getting the expiriation JWT claim" in {
      metadata("_akkasls-jwt-claim-exp" -> "12345").jwtClaims.expirationTime().toScala.value shouldBe Instant
        .ofEpochSecond(12345)
    }

    "support parsing object JWT claims" in {
      val jsonNode =
        metadata("_akkasls-jwt-claim-my-object" -> """{"foo":"bar"}""").jwtClaims().getObject("my-object").toScala.value
      jsonNode.get("foo").textValue() shouldBe "bar"
    }

    "support parsing string list JWT claims" in {
      val list = metadata("_akkasls-jwt-claim-my-string-list" -> """["foo","bar"]""")
        .jwtClaims()
        .getStringList("my-string-list")
        .toScala
        .value
      (list.asScala should contain).theSameElementsInOrderAs(List("foo", "bar"))
    }

    "support parsing int list JWT claims" in {
      val list = metadata("_akkasls-jwt-claim-my-int-list" -> """[3,4]""")
        .jwtClaims()
        .getIntegerList("my-int-list")
        .toScala
        .value
      (list.asScala should contain).theSameElementsInOrderAs(List(3, 4))
    }

    "ignore claims that are not the right type" in {
      val meta = metadata("_akkasls-jwt-claim-foo" -> "bar")
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
      val meta = metadata("_akkasls-jwt-claim-x" -> "bar")
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

  }

  private def metadata(entries: (String, String)*): Metadata = {
    new MetadataImpl(entries.map { case (key, value) =>
      MetadataEntry(key, MetadataEntry.Value.StringValue(value))
    })
  }

}
