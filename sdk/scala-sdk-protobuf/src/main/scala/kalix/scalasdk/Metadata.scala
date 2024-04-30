/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import kalix.javasdk.impl.{ MetadataImpl => JMetadataImpl }
import kalix.scalasdk.impl.MetadataImpl

import java.net.URI
import java.nio.ByteBuffer

/**
 * Transport specific metadata.
 *
 * <p>The exact semantics of how metadata is handled depends on the underlying transport. This API exposes case
 * insensitive lookups on metadata, but maintains the original case of the keys as received or inserted. If case
 * matters, the iterator should be used to access elements.
 *
 * <p>Multiple values are also supported per key, if the underlying transport does not support multiple values per key,
 * which value will be used is undefined.
 *
 * <p>Metadata can either have a string or a binary value. If the underlying transport doesn't support one or the other,
 * how those values are handled is undefined - eg, text values may be UTF-8 encoded in binary, or binary values may be
 * Base64 encoded, it depends on the transport.
 *
 * <p>This API maintains the order of entries, but the underlying transport may not.
 *
 * <p>Implementations of this class should be immutable, all update operations should return a copy of the metadata.
 */
trait Metadata extends Iterable[MetadataEntry] {
  private[kalix] def impl: JMetadataImpl

  /**
   * Get the string value for the given key, if found.
   *
   * <p>If the entry is a binary entry, nothing will be returned.
   *
   * <p>The key lookup is case insensitive. If multiple entries with the same key are present, the first string entry
   * will be returned.
   *
   * @param key
   *   The key to lookup.
   * @return
   *   The value, if found.
   */
  def get(key: String): Option[String]

  /**
   * Get all the string values for a given key.
   *
   * <p>Binary values will be ignored. The key lookup is case insensitive.
   *
   * @param key
   *   The key to lookup.
   * @return
   *   A list of all the string values for the given key.
   */
  def getAll(key: String): Seq[String]

  /**
   * Get the binary value for the given key, if found.
   *
   * <p>If the entry is a string entry, nothing will be returned.
   *
   * <p>The key lookup is case insensitive. If multiple entries with the same key are present, the first binary entry
   * will be returned.
   *
   * @param key
   *   The key to lookup.
   * @return
   *   The value, if found.
   */
  def getBinary(key: String): Option[ByteBuffer]

  /**
   * Get all the binary values for a given key.
   *
   * <p>String values will be ignored. The key lookup is case insensitive.
   *
   * @param key
   *   The key to lookup.
   * @return
   *   A list of all the binary values for the given key.
   */
  def getBinaryAll(key: String): Seq[ByteBuffer]

  /**
   * Check whether this metadata has a entry for the given key.
   *
   * <p>The key lookup will be case insensitive.
   *
   * @param key
   *   The key to lookup.
   * @return
   *   True if an entry for the given key exists, otherwise false.
   */
  def has(key: String): Boolean

  /**
   * Get all the keys for all the entries.
   *
   * <p>This list may contain duplicate keys if there are multiple entries with the same key.
   *
   * <p>The case of the keys will be the case as passed from the proxy or from other APIs.
   *
   * @return
   *   A list of all the keys in this metadata.
   */
  def getAllKeys(): Seq[String]

  /**
   * Set the string value for the given key.
   *
   * <p>This will replace any existing entries that case insensitively match the given key.
   *
   * <p>This method does not modify this Metadata object, it returns a copy of this Metadata object with the entry set.
   *
   * @param key
   *   The key to set.
   * @param value
   *   The value to set.
   * @return
   *   A copy of this Metadata object with the entry set.
   */
  def set(key: String, value: String): Metadata

  /**
   * Set the binary value for the given key.
   *
   * <p>This will replace any existing entries that case insensitively match the given key.
   *
   * <p>This method does not modify this Metadata object, it returns a copy of this Metadata object with the entry set.
   *
   * @param key
   *   The key to set.
   * @param value
   *   The value to set.
   * @return
   *   A copy of this Metadata object with the entry set.
   */
  def setBinary(key: String, value: ByteBuffer): Metadata

  /**
   * Add the string value for the given key.
   *
   * <p>This will not replace any existing entries, it will simply append the entry to the end of the list.
   *
   * <p>This method does not modify this Metadata object, it returns a copy of this Metadata object with the entry
   * added.
   *
   * @param key
   *   The key to add.
   * @param value
   *   The value to add.
   * @return
   *   A copy of this Metadata object with the entry added.
   */
  def add(key: String, value: String): Metadata

  /**
   * Add the binary value for the given key.
   *
   * <p>This will not replace any existing entries, it will simply append the entry to the end of the list.
   *
   * <p>This method does not modify this Metadata object, it returns a copy of this Metadata object with the entry
   * added.
   *
   * @param key
   *   The key to add.
   * @param value
   *   The value to add.
   * @return
   *   A copy of this Metadata object with the entry added.
   */
  def addBinary(key: String, value: ByteBuffer): Metadata

  /**
   * Remove all metadata entries with the given key.
   *
   * <p>The key will be matched against entries case insensitively.
   *
   * <p>This method does not modify this Metadata object, it returns a copy of this Metadata object with the entries
   * removed.
   *
   * @param key
   *   The key to remove.
   * @return
   *   A copy of this Metadata object with the entries removed.
   */
  def remove(key: String): Metadata

  /**
   * Clear all metadata entries.
   *
   * <p>This method does not modify this Metadata object, it returns an empty Metadata object.
   *
   * @return
   *   An empty metadata object.
   */
  def clear(): Metadata

  /**
   * Whether this metadata is also a CloudEvent.
   *
   * <p>This will return true if all of the required CloudEvent fields are set, that is, the specversion, id, source and
   * type fields.
   *
   * @return
   *   True if the CloudEvent required attributes are set in this Metadata.
   */
  def isCloudEvent: Boolean

  /**
   * Return a CloudEvent representation of this Metadata.
   *
   * <p>Note that the CloudEvent representation will retain any non CloudEvent metadata when converted back to Metadata.
   *
   * @return
   *   This Metadata expressed as CloudEvent metadata.
   * @throws java.lang.IllegalStateException
   *   If this metadata is not a CloudEvent, that is, if it doesn't have any of specversion, id, source or type
   *   CloudEvent fields defined.
   */
  def asCloudEvent: CloudEvent

  /**
   * Convert this metadata to a CloudEvent, adding the given required CloudEvent fields.
   *
   * <p>Any metadata in this Metadata object will be left intact when asMetadata is called
   *
   * @param id
   *   The id of the CloudEvent.
   * @param source
   *   The source of the CloudEvent.
   * @param type
   *   The type of the CloudEvent.
   * @return
   *   This metadata, represented as a CloudEvent with the specified fields.
   */
  def asCloudEvent(id: String, source: URI, `type`: String): CloudEvent

  /**
   * Get the JWT claims present in this metadata.
   *
   * @return
   *   The JWT claims.
   */
  def jwtClaims: JwtClaims

  /**
   * Get the Principals associated with this request.
   */
  def principals: Principals

  /**
   * Add an HTTP response code to this metadata. This will only take effect when HTTP transcoding is in use. It will be
   * ignored for gRPC requests.
   *
   * @param httpStatusCode
   *   The success status code to add.
   * @return
   *   a copy of this metadata with the HTTP response code set.
   */
  def withStatusCode(httpStatusCode: StatusCode.Success): Metadata

  /**
   * Add an HTTP response code to this metadata. This will only take effect when HTTP transcoding is in use. It will be
   * ignored for gRPC requests.
   *
   * @param httpStatusCode
   *   The redirect status code to add.
   * @return
   *   a copy of this metadata with the HTTP response code set.
   */
  def withStatusCode(httpStatusCode: StatusCode.Redirect): Metadata

  /**
   * Get the trace context associated with this request metadata.
   * @return
   *   The trace context.
   */
  def traceContext: TraceContext
}

object Metadata {
  val empty: Metadata = MetadataImpl(JMetadataImpl.Empty)
}

/** A metadata entry. */
trait MetadataEntry {

  /**
   * The key for the metadata entry.
   *
   * <p>The key will be in the original case it was inserted or sent as.
   *
   * @return
   *   The key.
   */
  def key: String

  /**
   * The string value for the metadata entry.
   *
   * @return
   *   The string value, or null if this entry is not a string Metadata entry.
   */
  def value: String

  /**
   * The binary value for the metadata entry.
   *
   * @return
   *   The binary value, or null if this entry is not a string Metadata entry.
   */
  def binaryValue: ByteBuffer

  /**
   * Whether this entry is a text entry.
   *
   * @return
   *   True if this entry is a text entry.
   */
  def isText: Boolean

  /**
   * Whether this entry is a binary entry.
   *
   * @return
   *   True if this entry is a binary entry.
   */
  def isBinary: Boolean
}
