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

package com.akkaserverless.scalasdk.impl
import java.nio.ByteBuffer
import scala.collection.immutable
import com.akkaserverless.javasdk.impl.{ MetadataImpl => Impl }
import com.akkaserverless.scalasdk.CloudEvent
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.MetadataEntry
import com.akkaserverless.protocol.component.{ MetadataEntry => ProtocolMetadataEntry }

private[akkaserverless] object MetadataImpl {
  def apply(impl: com.akkaserverless.javasdk.impl.MetadataImpl): MetadataImpl = new MetadataImpl(impl)
  def apply(entries: immutable.Seq[ProtocolMetadataEntry]): MetadataImpl = MetadataImpl(
    new com.akkaserverless.javasdk.impl.MetadataImpl(entries))
}

private[akkaserverless] class MetadataImpl(val impl: com.akkaserverless.javasdk.impl.MetadataImpl)
    extends Metadata
    with CloudEvent {
  def asMetadata: Metadata = this
  def clearDatacontenttype(): CloudEvent = MetadataImpl(impl.clearDatacontenttype()).asCloudEvent
  def clearDataschema(): CloudEvent = MetadataImpl(impl.clearDataschema()).asCloudEvent
  def clearSubject(): CloudEvent = MetadataImpl(impl.clearSubject()).asCloudEvent
  def clearTime(): CloudEvent = MetadataImpl(impl.clearTime()).asCloudEvent
  def datacontenttype: Option[String] = impl.datacontenttypeScala
  def dataschema: Option[java.net.URI] = impl.dataschemaScala
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
}
