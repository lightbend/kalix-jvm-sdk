/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.{ Context => OtelContext }
import kalix.scalasdk.TraceContext
import kalix.javasdk.impl.telemetry.TraceInstrumentation
import kalix.protocol.component.{ MetadataEntry => ProtocolMetadataEntry }
import kalix.scalasdk._

import java.nio.ByteBuffer
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

private[kalix] object MetadataImpl {
  def apply(impl: kalix.javasdk.impl.MetadataImpl): MetadataImpl = new MetadataImpl(impl)
  def apply(entries: Seq[ProtocolMetadataEntry]): MetadataImpl = MetadataImpl(
    kalix.javasdk.impl.MetadataImpl.of(entries))
}

private[kalix] class MetadataImpl(val impl: kalix.javasdk.impl.MetadataImpl) extends Metadata with CloudEvent {
  def asMetadata: Metadata = this
  def clearDatacontenttype(): CloudEvent = MetadataImpl(impl.clearDatacontenttype()).asCloudEvent
  def clearDataschema(): CloudEvent = MetadataImpl(impl.clearDataschema()).asCloudEvent
  def clearSubject(): CloudEvent = MetadataImpl(impl.clearSubject()).asCloudEvent
  def clearTime(): CloudEvent = MetadataImpl(impl.clearTime()).asCloudEvent
  def datacontenttype: Option[String] = impl.datacontenttypeScala()
  def dataschema: Option[java.net.URI] = impl.dataschemaScala()
  def id: String = impl.id
  def source: java.net.URI = impl.source
  def specversion: String = impl.specversion
  def subject: Option[String] = impl.subjectScala
  def time: Option[java.time.ZonedDateTime] = impl.timeScala
  def withDatacontenttype(datacontenttype: String): CloudEvent = MetadataImpl(
    impl.withDatacontenttype(datacontenttype)).asCloudEvent
  def withDataschema(dataschema: java.net.URI): CloudEvent = MetadataImpl(impl.withDataschema(dataschema)).asCloudEvent
  def withId(id: String): CloudEvent = MetadataImpl(impl.withId(id)).asCloudEvent
  def withSource(source: java.net.URI): CloudEvent = MetadataImpl(impl.withSource(source)).asCloudEvent
  def withSubject(subject: String): CloudEvent = MetadataImpl(impl.withSubject(subject)).asCloudEvent
  def withTime(time: java.time.ZonedDateTime): CloudEvent = MetadataImpl(impl.withTime(time)).asCloudEvent
  def withType(`type`: String): CloudEvent = MetadataImpl(impl.withType(`type`)).asCloudEvent
  def `type`: String = impl.`type`

  def iterator: Iterator[MetadataEntry] = {
    impl
      .iteratorScala(entry =>
        new MetadataEntry {
          override def key: String = entry.key
          override def value: String = entry.value.stringValue.orNull
          override def binaryValue: ByteBuffer = entry.value.bytesValue.map(_.asReadOnlyByteBuffer()).orNull
          override def isText: Boolean = entry.value.isStringValue
          override def isBinary: Boolean = entry.value.isBytesValue
        })
  }
  def add(key: String, value: String): Metadata = MetadataImpl(impl.add(key, value))
  def addBinary(key: String, value: java.nio.ByteBuffer): Metadata = MetadataImpl(impl.addBinary(key, value))
  def asCloudEvent: CloudEvent = MetadataImpl(impl.asCloudEvent)
  def asCloudEvent(id: String, source: java.net.URI, `type`: String): CloudEvent = MetadataImpl(
    impl.asCloudEvent(id, source, `type`))
  def clear(): Metadata = MetadataImpl(impl.clear())
  def get(key: String): Option[String] = impl.getScala(key)
  def getAll(key: String): Seq[String] = impl.getAllScala(key)
  def getAllKeys(): Seq[String] = impl.getAllKeysScala
  def getBinary(key: String): Option[java.nio.ByteBuffer] = impl.getBinaryScala(key)
  def getBinaryAll(key: String): Seq[java.nio.ByteBuffer] = impl.getBinaryAllScala(key)
  def has(key: String): Boolean = impl.has(key)
  def isCloudEvent: Boolean = impl.isCloudEvent
  def remove(key: String): Metadata = MetadataImpl(impl.remove(key))
  def set(key: String, value: String): Metadata = MetadataImpl(impl.set(key, value))
  def setBinary(key: String, value: java.nio.ByteBuffer): Metadata = MetadataImpl(impl.setBinary(key, value))
  // The reason we don't just implement JwtClaims ourselves is that some of the methods clash with CloudEvent
  override lazy val jwtClaims: JwtClaims = new JwtClaims {
    override def allClaimNames: Iterable[String] = impl.allJwtClaimNames
    override def asMap: Map[String, String] = impl.jwtClaimsAsMap
    override def getString(name: String): Option[String] = impl.getJwtClaim(name)
  }
  override lazy val principals: Principals = new Principals {
    override def isInternet: Boolean = impl.principals.isInternet
    override def isSelf: Boolean = impl.principals.isSelf
    override def isBackoffice: Boolean = impl.principals.isBackoffice
    override def isLocalService(name: String): Boolean = impl.principals.isLocalService(name)
    override def isAnyLocalService: Boolean = impl.principals.isAnyLocalService
    override def localService: Option[String] = impl.principals.getLocalService.toScala
    override def apply: Seq[Principal] = impl.principals.get.asScala.map(Principal.toScala).toSeq
  }
  override def withStatusCode(code: StatusCode.Success): Metadata =
    set("_kalix-http-code", code.value.toString)

  override def withStatusCode(code: StatusCode.Redirect): Metadata =
    set("_kalix-http-code", code.value.toString)

  override lazy val traceContext: TraceContext = new TraceContext {
    override def asOpenTelemetryContext = W3CTraceContextPropagator
      .getInstance()
      .extract(OtelContext.current(), asMetadata, otelGetter)

    override def traceParent: Option[String] = get(TraceInstrumentation.TRACE_PARENT_KEY)

    override def traceState: Option[String] = get(TraceInstrumentation.TRACE_STATE_KEY)
  }

  lazy val otelGetter = new TextMapGetter[Metadata]() {
    override def get(carrier: Metadata, key: String): String = {
      carrier.get(key).getOrElse("")
    }

    override def keys(carrier: Metadata): java.lang.Iterable[String] =
      carrier.getAllKeys().asJava
  }
}
