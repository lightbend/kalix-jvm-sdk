/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import java.net.URI
import java.time.ZonedDateTime

/** CloudEvent representation of Metadata. */
trait CloudEvent {

  /**
   * The CloudEvent spec version.
   *
   * @return
   *   The spec version.
   */
  def specversion: String

  /**
   * The id of this CloudEvent.
   *
   * @return
   *   The id.
   */
  def id: String

  /**
   * Return a new CloudEvent with the given id.
   *
   * @param id
   *   The id to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withId(id: String): CloudEvent

  /**
   * The source of this CloudEvent.
   *
   * @return
   *   The source.
   */
  def source: URI

  /**
   * Return a new CloudEvent with the given source.
   *
   * @param source
   *   The source to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withSource(source: URI): CloudEvent

  /**
   * The type of this CloudEvent.
   *
   * @return
   *   The type.
   */
  def `type`: String

  /**
   * Return a new CloudEvent with the given type.
   *
   * @param type
   *   The type to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withType(`type`: String): CloudEvent

  /**
   * The data content type of this CloudEvent.
   *
   * @return
   *   The data content type, if set.
   */
  def datacontenttype: Option[String]

  /**
   * Return a new CloudEvent with the given data content type.
   *
   * @param datacontenttype
   *   The data content type to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withDatacontenttype(datacontenttype: String): CloudEvent

  /**
   * Clear the data content type of this CloudEvent, if set.
   *
   * @return
   *   A copy of this CloudEvent.
   */
  def clearDatacontenttype(): CloudEvent

  /**
   * The data schema of this CloudEvent.
   *
   * @return
   *   The data schema, if set.
   */
  def dataschema: Option[URI]

  /**
   * Return a new CloudEvent with the given data schema.
   *
   * @param dataschema
   *   The data schema to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withDataschema(dataschema: URI): CloudEvent

  /**
   * Clear the data schema of this CloudEvent, if set.
   *
   * @return
   *   A copy of this CloudEvent.
   */
  def clearDataschema(): CloudEvent

  /**
   * The subject of this CloudEvent.
   *
   * @return
   *   The subject, if set.
   */
  def subject: Option[String]

  /**
   * Return a new CloudEvent with the given subject.
   *
   * @param subject
   *   The subject to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withSubject(subject: String): CloudEvent

  /**
   * Clear the subject of this CloudEvent, if set.
   *
   * @return
   *   A copy of this CloudEvent.
   */
  def clearSubject(): CloudEvent

  /**
   * The time of this CloudEvent.
   *
   * @return
   *   The time, if set.
   */
  def time: Option[ZonedDateTime]

  /**
   * Return a new CloudEvent with the given time.
   *
   * @param time
   *   The time to set.
   * @return
   *   A copy of this CloudEvent.
   */
  def withTime(time: ZonedDateTime): CloudEvent

  /**
   * Clear the time of this CloudEvent, if set.
   *
   * @return
   *   A copy of this CloudEvent.
   */
  def clearTime(): CloudEvent

  /**
   * Return this CloudEvent represented as Metadata.
   *
   * <p>If this CloudEvent was created by [[kalix.scalasdk.Metadata.asCloudEvent:*]], then any non CloudEvent metadata
   * that was present will still be present.
   *
   * @return
   *   This CloudEvent expressed as Kalix metadata.
   */
  def asMetadata: Metadata
}

object CloudEvent {

  /**
   * Create a CloudEvent.
   *
   * @param id
   *   The id of the CloudEvent.
   * @param source
   *   The source of the CloudEvent.
   * @param type
   *   The type of the CloudEvent.
   * @return
   *   The newly created CloudEvent.
   */
  def apply(id: String, source: URI, `type`: String): CloudEvent = {
    Metadata.empty.asCloudEvent(id, source, `type`)
  }
}
