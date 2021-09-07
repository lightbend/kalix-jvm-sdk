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

package com.akkaserverless.javasdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;

import java.io.IOException;
import java.util.Optional;

public final class JsonSupport {

  public static final String AKKA_SERVERLESS_JSON = "json.akkaserverless.com/";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private JsonSupport() {};

  /**
   * Encode the given value as JSON using Jackson and put the encoded string as bytes in a protobuf
   * Any with the type URL {@code "json.akkaserverless.com/[valueClassName]"}.
   *
   * <p>Note that if the serialized Any is published to a pub/sub topic that is consumed by an
   * external service using the class name suffix this introduces coupling as the internal class
   * name of this service becomes known to the outside of the service (and for exampe renaming it
   * may break existing consumers). For such cases consider using the overload with an explicit name
   * for the JSON type instead.
   *
   * @see {{encodeJson(T, String}}
   */
  public static <T> Any encodeJson(T value) {
    return encodeJson(value, value.getClass().getName());
  }

  /**
   * Encode the given value as JSON using Jackson and put the encoded string as bytes in a protobuf
   * Any with the type URL {@code "json.akkaserverless.com/[jsonType]"}.
   *
   * @param value the object to encode as JSON, must be an instance of a class properly annotated
   *     with the needed Jackson annotations.
   * @param jsonType A discriminator making it possible to identify which type of object is in the
   *     JSON, useful for example when multiple different objects are passed through a pub/sub
   *     topic.
   * @throws IllegalArgumentException if the given value cannot be turned into JSON
   */
  public static <T> Any encodeJson(T value, String jsonType) {
    try {
      ByteString json = UnsafeByteOperations.unsafeWrap(objectMapper.writeValueAsBytes(value));
      return Any.newBuilder().setTypeUrl(AKKA_SERVERLESS_JSON + jsonType).setValue(json).build();
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException(
          "Could not encode [" + value.getClass().getName() + "] as JSON", ex);
    }
  }

  /**
   * Decode the given protobuf Any object to an instance of T using Jackson. The object must have
   * the JSON string as bytes as value and a type URL starting with "json.akkaserverless.com/".
   *
   * @param valueClass The type of class to deserialize the object to, the class must have the
   *     proper Jackson annotations for deserialization.
   * @return The decoded object
   * @throws IllegalArgumentException if the given value cannot be decoded to a T
   */
  public static <T> T decodeJson(Class<T> valueClass, Any any) {
    if (!any.getTypeUrl().startsWith(AKKA_SERVERLESS_JSON)) {
      throw new IllegalArgumentException(
          "Protobuf bytes with type url ["
              + any.getTypeUrl()
              + "] cannot be decoded as JSON, must start with ["
              + AKKA_SERVERLESS_JSON
              + "]");
    } else {
      try {
        return objectMapper.readerFor(valueClass).readValue(any.getValue().toByteArray());
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "JSON with type url ["
                + any.getTypeUrl()
                + "] could not be decoded into a ["
                + valueClass.getName()
                + "]");
      }
    }
  }

  /**
   * Decode the given protobuf Any to an instance of T using Jackson but only if the suffix of the
   * type URL matches the given jsonType.
   *
   * @return An Optional containing the successfully decoded value or an empty Optional if the type
   *     suffix does not match.
   * @throws IllegalArgumentException if the suffix matches but the Any cannot be parsed into a T
   */
  public static <T> Optional<T> decodeJson(Class<T> valueClass, String jsonType, Any any) {
    if (any.getTypeUrl().endsWith(jsonType)) {
      return Optional.of(decodeJson(valueClass, any));
    } else {
      return Optional.empty();
    }
  }
}
