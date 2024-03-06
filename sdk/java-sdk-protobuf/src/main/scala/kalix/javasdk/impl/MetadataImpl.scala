/*
 * Copyright 2024 Lightbend Inc.
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

import com.google.protobuf.ByteString
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import kalix.javasdk.CloudEvent
import kalix.javasdk.JwtClaims
import kalix.javasdk.Metadata
import kalix.javasdk.Principal
import kalix.javasdk.Principals
import kalix.javasdk.StatusCode
import kalix.javasdk.TraceContext
import kalix.javasdk.impl.MetadataImpl.JwtClaimPrefix
import kalix.protocol.component
import kalix.protocol.component.MetadataEntry
import io.opentelemetry.context.{ Context => OtelContext }
import kalix.javasdk.impl.telemetry.TraceInstrumentation
import kalix.javasdk.impl.telemetry.TraceInstrumentation.otelGetter

import java.lang
import java.net.URI
import java.nio.ByteBuffer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util
import java.util.Objects
import java.util.Optional
import scala.compat.java8.OptionConverters._
import scala.jdk.CollectionConverters._

private[kalix] class MetadataImpl private (val entries: Seq[MetadataEntry]) extends Metadata with CloudEvent {

  override def has(key: String): Boolean = entries.exists(_.key.equalsIgnoreCase(key))

  override def get(key: String): Optional[String] =
    getScala(key).asJava

  private[kalix] def getScala(key: String): Option[String] =
    entries.collectFirst {
      case MetadataEntry(k, MetadataEntry.Value.StringValue(value), _) if key.equalsIgnoreCase(k) =>
        value
    }

  override def getAll(key: String): util.List[String] =
    getAllScala(key).asJava

  private[kalix] def getAllScala(key: String): Seq[String] =
    entries.collect {
      case MetadataEntry(k, MetadataEntry.Value.StringValue(value), _) if key.equalsIgnoreCase(k) =>
        value
    }

  override def getBinary(key: String): Optional[ByteBuffer] =
    getBinaryScala(key).asJava

  private[kalix] def getBinaryScala(key: String): Option[ByteBuffer] =
    entries.collectFirst {
      case MetadataEntry(k, MetadataEntry.Value.BytesValue(value), _) if key.equalsIgnoreCase(k) =>
        value.asReadOnlyByteBuffer()
    }

  override def getBinaryAll(key: String): util.List[ByteBuffer] =
    getBinaryAllScala(key).asJava

  private[kalix] def getBinaryAllScala(key: String): Seq[ByteBuffer] =
    entries.collect {
      case MetadataEntry(k, MetadataEntry.Value.BytesValue(value), _) if key.equalsIgnoreCase(k) =>
        value.asReadOnlyByteBuffer()
    }

  override def getAllKeys: util.List[String] = getAllKeysScala.asJava
  private[kalix] def getAllKeysScala: Seq[String] = entries.map(_.key)

  override def set(key: String, value: String): MetadataImpl = {
    Objects.requireNonNull(key, "Key must not be null")
    Objects.requireNonNull(value, "Value must not be null")
    MetadataImpl.of(removeKey(key) :+ MetadataEntry(key, MetadataEntry.Value.StringValue(value)))
  }

  override def setBinary(key: String, value: ByteBuffer): MetadataImpl = {
    Objects.requireNonNull(key, "Key must not be null")
    Objects.requireNonNull(value, "Value must not be null")
    MetadataImpl.of(removeKey(key) :+ MetadataEntry(key, MetadataEntry.Value.BytesValue(ByteString.copyFrom(value))))
  }

  override def add(key: String, value: String): MetadataImpl = {
    Objects.requireNonNull(key, "Key must not be null")
    Objects.requireNonNull(value, "Value must not be null")
    MetadataImpl.of(entries :+ MetadataEntry(key, MetadataEntry.Value.StringValue(value)))
  }

  override def addBinary(key: String, value: ByteBuffer): MetadataImpl = {
    Objects.requireNonNull(key, "Key must not be null")
    Objects.requireNonNull(value, "Value must not be null")
    MetadataImpl.of(entries :+ MetadataEntry(key, MetadataEntry.Value.BytesValue(ByteString.copyFrom(value))))
  }

  override def remove(key: String): MetadataImpl = MetadataImpl.of(removeKey(key))

  override def clear(): MetadataImpl = MetadataImpl.Empty

  private[kalix] def iteratorScala[R](f: MetadataEntry => R): Iterator[R] =
    entries.iterator.map(f)

  override def iterator(): util.Iterator[Metadata.MetadataEntry] =
    iteratorScala(entry =>
      new Metadata.MetadataEntry {
        override def getKey: String = entry.key
        override def getValue: String = entry.value.stringValue.orNull
        override def getBinaryValue: ByteBuffer = entry.value.bytesValue.map(_.asReadOnlyByteBuffer()).orNull
        override def isText: Boolean = entry.value.isStringValue
        override def isBinary: Boolean = entry.value.isBytesValue
      }).asJava

  private def removeKey(key: String) = entries.filterNot(_.key.equalsIgnoreCase(key))

  def isCloudEvent: Boolean = MetadataImpl.CeRequired.forall(h => has(h))

  override def asCloudEvent(): MetadataImpl =
    if (!isCloudEvent) {
      throw new IllegalStateException("Metadata is not a CloudEvent!")
    } else this

  override def asCloudEvent(id: String, source: URI, `type`: String): MetadataImpl =
    MetadataImpl.of(
      entries.filterNot(e => MetadataImpl.CeRequired(e.key)) ++
      Seq(
        MetadataEntry(MetadataImpl.CeSpecversion, MetadataEntry.Value.StringValue(MetadataImpl.CeSpecversionValue)),
        MetadataEntry(MetadataImpl.CeId, MetadataEntry.Value.StringValue(id)),
        MetadataEntry(MetadataImpl.CeSource, MetadataEntry.Value.StringValue(source.toString)),
        MetadataEntry(MetadataImpl.CeType, MetadataEntry.Value.StringValue(`type`))))

  private def getRequiredCloudEventField(key: String) =
    entries
      .collectFirst {
        case MetadataEntry(k, MetadataEntry.Value.StringValue(value), _) if key.equalsIgnoreCase(k) =>
          value
      }
      .getOrElse {
        throw new IllegalStateException(s"Metadata is not a CloudEvent because it does not have required field $key")
      }

  override def specversion(): String = getRequiredCloudEventField(MetadataImpl.CeSpecversion)

  override def id(): String = getRequiredCloudEventField(MetadataImpl.CeId)

  override def withId(id: String): MetadataImpl = set(MetadataImpl.CeId, id)

  override def source(): URI = URI.create(getRequiredCloudEventField(MetadataImpl.CeSource))

  override def withSource(source: URI): MetadataImpl = set(MetadataImpl.CeSource, source.toString)

  override def `type`(): String = getRequiredCloudEventField(MetadataImpl.CeType)

  override def withType(`type`: String): MetadataImpl = set(MetadataImpl.CeType, `type`)

  override def datacontenttype(): Optional[String] = getScala(MetadataImpl.CeDatacontenttype).asJava
  private[kalix] def datacontenttypeScala(): Option[String] = getScala(MetadataImpl.CeDatacontenttype)

  override def withDatacontenttype(datacontenttype: String): MetadataImpl =
    set(MetadataImpl.CeDatacontenttype, datacontenttype)

  override def clearDatacontenttype(): MetadataImpl = remove(MetadataImpl.CeDatacontenttype)

  override def dataschema(): Optional[URI] = dataschemaScala().asJava
  private[kalix] def dataschemaScala(): Option[URI] = getScala(MetadataImpl.CeDataschema).map(URI.create(_))

  override def withDataschema(dataschema: URI): MetadataImpl = set(MetadataImpl.CeDataschema, dataschema.toString)

  override def clearDataschema(): MetadataImpl = remove(MetadataImpl.CeDataschema)

  override def subject(): Optional[String] = subjectScala.asJava
  private[kalix] def subjectScala: Option[String] = getScala(MetadataImpl.CeSubject)

  override def withSubject(subject: String): MetadataImpl = set(MetadataImpl.CeSubject, subject)

  override def clearSubject(): MetadataImpl = remove(MetadataImpl.CeSubject)

  override def time(): Optional[ZonedDateTime] = timeScala.asJava
  private[kalix] def timeScala: Option[ZonedDateTime] =
    getScala(MetadataImpl.CeTime).map(ZonedDateTime.parse(_))

  override def withTime(time: ZonedDateTime): MetadataImpl =
    set(MetadataImpl.CeTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time))

  override def clearTime(): MetadataImpl = remove(MetadataImpl.CeTime)

  override def withStatusCode(code: StatusCode.Redirect): MetadataImpl =
    set("_kalix-http-code", code.value.toString)

  override def withStatusCode(code: StatusCode.Success): MetadataImpl =
    set("_kalix-http-code", code.value.toString)

  override def asMetadata(): Metadata = this

  // The reason we don't just implement JwtClaims ourselves is that some of the methods clash with CloudEvent
  override lazy val jwtClaims: JwtClaims = new JwtClaims {
    override def allClaimNames(): lang.Iterable[String] = allJwtClaimNames.asJava
    override def asMap(): util.Map[String, String] = jwtClaimsAsMap.asJava
    override def getString(name: String): Optional[String] = getJwtClaim(name).asJava
  }

  override lazy val principals: Principals = new Principals {
    private def src: Option[String] = getScala(MetadataImpl.PrincipalsSource)
    private def svc: Option[String] = getScala(MetadataImpl.PrincipalsService)
    override def isInternet: Boolean = src.contains("internet")
    override def isSelf: Boolean = src.contains("self")
    override def isBackoffice: Boolean = src.contains("backoffice")
    override def isLocalService(name: String): Boolean = svc.contains(name)
    override def isAnyLocalService: Boolean = svc.nonEmpty
    override def getLocalService: Optional[String] = svc.asJava
    override def get(): util.Collection[Principal] = {
      (src.collect {
        case "internet"   => Principal.INTERNET
        case "self"       => Principal.SELF
        case "backoffice" => Principal.BACKOFFICE
      } ++ svc.map(Principal.localService)).asJavaCollection
    }
  }

  override lazy val traceContext: TraceContext = new TraceContext {
    override def asOpenTelemetryContext(): OtelContext = W3CTraceContextPropagator
      .getInstance()
      .extract(OtelContext.current(), asMetadata(), otelGetter)

    override def traceParent(): Optional[String] = getScala(TraceInstrumentation.TRACE_PARENT_KEY).asJava

    override def traceState(): Optional[String] = getScala(TraceInstrumentation.TRACE_STATE_KEY).asJava
  }

  private[kalix] def allJwtClaimNames: Iterable[String] =
    entries.view.collect {
      case MetadataEntry(key, MetadataEntry.Value.StringValue(_), _) if key.startsWith(JwtClaimPrefix) => key
    }

  private[kalix] def jwtClaimsAsMap: Map[String, String] =
    entries.view.collect {
      case MetadataEntry(key, MetadataEntry.Value.StringValue(value), _) if key.startsWith(JwtClaimPrefix) =>
        key -> value
    }.toMap

  private[kalix] def getJwtClaim(name: String): Option[String] = {
    val prefixedName = JwtClaimPrefix + name
    entries.collectFirst {
      case MetadataEntry(key, MetadataEntry.Value.StringValue(value), _) if key == prefixedName => value
    }
  }
}

object MetadataImpl {
  val CeSpecversion = "ce-specversion"
  val CeSpecversionValue = "1.0"
  val CeId = "ce-id"
  val CeSource = "ce-source"
  val CeType = "ce-type"
  // As per CloudEvent HTTP encoding spec, we use Content-Type to encode this.
  val CeDatacontenttype = "Content-Type"
  val CeDataschema = "ce-dataschema"
  val CeSubject = "ce-subject"
  val CeTime = "ce-time"
  val CeRequired: Set[String] = Set(CeSpecversion, CeId, CeSource, CeType)
  private val AllCeAttributes = CeRequired ++ Set(CeDataschema, CeDatacontenttype, CeSubject, CeTime)

  /**
   * Maps alternative prefixed keys to our default key format, ie: ce-.
   *
   * For the moment, only the Kafka prefix is in use, ie: ce_, but others might be needed in future.
   */
  private val alternativeKeyFormats = AllCeAttributes.map { attr =>
    val key = attr.replaceFirst("^ce-", "ce_")
    (key, attr)
  }.toMap

  val Empty = MetadataImpl.of(Vector.empty)

  val JwtClaimPrefix = "_kalix-jwt-claim-"

  val PrincipalsSource = "_kalix-src"
  val PrincipalsService = "_kalix-src-svc"

  def toProtocol(metadata: kalix.javasdk.Metadata): Option[component.Metadata] =
    metadata match {
      case impl: MetadataImpl if impl.entries.nonEmpty =>
        Some(component.Metadata(impl.entries))
      case _: MetadataImpl => None
      case other =>
        throw new RuntimeException(s"Unknown metadata implementation: ${other.getClass}, cannot send")
    }

  def of(entries: Seq[MetadataEntry]): MetadataImpl = {
    val transformedEntries =
      entries.map { entry =>
        // is incoming ce key in one of the alternative formats?
        // if so, convert key to our internal default key format
        alternativeKeyFormats.get(entry.key) match {
          case Some(defaultKey) => MetadataEntry(defaultKey, entry.value)
          case _                => entry
        }
      }

    new MetadataImpl(transformedEntries)
  }
}
