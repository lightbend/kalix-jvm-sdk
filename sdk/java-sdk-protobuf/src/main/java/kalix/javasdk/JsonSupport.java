/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import akka.Done;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import kalix.javasdk.annotations.Migration;
import kalix.javasdk.impl.ByteStringEncoding;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;

public final class JsonSupport {

  public static final String KALIX_JSON = "json.kalix.io/";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    // Date/time in ISO-8601 (rfc3339) yyyy-MM-dd'T'HH:mm:ss.SSSZ format
    // as defined by com.fasterxml.jackson.databind.util.StdDateFormat
    // For interoperability it's better to use the ISO format, i.e. WRITE_DATES_AS_TIMESTAMPS=off,
    // but WRITE_DATES_AS_TIMESTAMPS=on has better performance.
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);

    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    // ParameterNamesModule needs the parameter to ensure that single-parameter
    // constructors are handled the same way as constructors with multiple parameters.
    // See https://github.com/FasterXML/jackson-module-parameter-names#delegating-creator
    objectMapper.registerModule(
        new com.fasterxml.jackson.module.paramnames.ParameterNamesModule(
            JsonCreator.Mode.PROPERTIES));
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    SimpleModule module = new SimpleModule();
    module.addSerializer(Done.class, new DoneSerializer());
    module.addDeserializer(Done.class, new DoneDeserializer());

    objectMapper.registerModule(module);
  }

  /**
   * The Jackson ObjectMapper that is used for encoding and decoding JSON. You may adjust it's
   * configuration, but that must only be performed before starting {@link Kalix}
   */
  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private JsonSupport() {
  }

  /**
   * Encode the given value as JSON using Jackson and put the encoded string as bytes in a protobuf
   * Any with the type URL {@code "json.kalix.io/[valueClassName]"}.
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
   * Any with the type URL {@code "json.kalix.io/[jsonType]"}.
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
      ByteString bytes = encodeToBytes(value);
      ByteString encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes);
      return Any.newBuilder().setTypeUrl(KALIX_JSON + jsonType).setValue(encodedBytes).build();
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException(
          "Could not encode [" + value.getClass().getName() + "] as JSON", ex);
    }
  }

  public static <T> ByteString encodeToBytes(T value) throws JsonProcessingException {
    return UnsafeByteOperations.unsafeWrap(
        objectMapper.writerFor(value.getClass()).writeValueAsBytes(value));
  }

  /**
   * Decode the given protobuf Any object to an instance of T using Jackson. The object must have
   * the JSON string as bytes as value and a type URL starting with "json.kalix.io/".
   *
   * @param valueClass The type of class to deserialize the object to, the class must have the
   *                   proper Jackson annotations for deserialization.
   * @param any        The protobuf Any object to deserialize.
   * @return The decoded object
   * @throws IllegalArgumentException if the given value cannot be decoded to a T
   */
  public static <T> T decodeJson(Class<T> valueClass, Any any) {
    if (!any.getTypeUrl().startsWith(KALIX_JSON)) {
      throw new IllegalArgumentException(
          "Protobuf bytes with type url ["
              + any.getTypeUrl()
              + "] cannot be decoded as JSON, must start with ["
              + KALIX_JSON
              + "]");
    } else {
      try {
        ByteString decodedBytes = ByteStringEncoding.decodePrimitiveBytes(any.getValue());
        if (valueClass.getAnnotation(Migration.class) != null) {
          JsonMigration migration = valueClass.getAnnotation(Migration.class)
              .value()
              .getConstructor()
              .newInstance();
          int fromVersion = parseVersion(any.getTypeUrl());
          int currentVersion = migration.currentVersion();
          int supportedForwardVersion = migration.supportedForwardVersion();
          if (fromVersion < currentVersion) {
            return migrate(valueClass, decodedBytes, fromVersion, migration);
          } else if (fromVersion == currentVersion) {
            return parseBytes(decodedBytes.toByteArray(), valueClass);
          } else if (fromVersion <= supportedForwardVersion) {
            return migrate(valueClass, decodedBytes, fromVersion, migration);
          } else {
            throw new IllegalStateException("Migration version " + supportedForwardVersion + " is " +
                "behind version " + fromVersion + " of deserialized type [" + valueClass.getName() + "]");
          }
        } else {
          return parseBytes(decodedBytes.toByteArray(), valueClass);
        }
      } catch (JsonProcessingException e) {
        throw jsonProcessingException(valueClass, any, e);
      } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException |
               InvocationTargetException e) {
        throw genericDecodeException(valueClass, any, e);
      }
    }
  }

  public static <T> T parseBytes(byte[] bytes, Class<T> valueClass) throws IOException {
    return objectMapper.readValue(bytes, valueClass);
  }

  private static <T> IllegalArgumentException jsonProcessingException(Class<T> valueClass, Any any, JsonProcessingException e) {
    return new IllegalArgumentException(
        "JSON with type url ["
            + any.getTypeUrl()
            + "] could not be decoded into a ["
            + valueClass.getName()
            + "]. Make sure that changes are backwards compatible or apply a @Migration mechanism (https://docs.kalix.io/java/serialization.html#_schema_evolution).",
        e);
  }

  private static <T> IllegalArgumentException genericDecodeException(Class<T> valueClass, Any any, Exception e) {
    return new IllegalArgumentException(
        "JSON with type url ["
            + any.getTypeUrl()
            + "] could not be decoded into a ["
            + valueClass.getName()
            + "]",
        e);
  }

  private static <T> T migrate(Class<T> valueClass, ByteString decodedBytes, int fromVersion, JsonMigration jsonMigration) throws IOException {
    JsonNode jsonNode = objectMapper.readTree(decodedBytes.toByteArray());
    JsonNode newJsonNode = jsonMigration.transform(fromVersion, jsonNode);
    return objectMapper.treeToValue(newJsonNode, valueClass);
  }

  private static int parseVersion(String typeUrl) {
    int versionSeparatorIndex = typeUrl.lastIndexOf("#");
    if (versionSeparatorIndex > 0) {
      String maybeVersion = typeUrl.substring(versionSeparatorIndex + 1);
      return Integer.parseInt(maybeVersion);
    } else {
      return 0;
    }
  }

  public static <T, C extends Collection<T>> C decodeJsonCollection(Class<T> valueClass, Class<C> collectionType, Any any) {
    if (!any.getTypeUrl().startsWith(KALIX_JSON)) {
      throw new IllegalArgumentException(
          "Protobuf bytes with type url ["
              + any.getTypeUrl()
              + "] cannot be decoded as JSON, must start with ["
              + KALIX_JSON
              + "]");
    } else {
      try {
        ByteString decodedBytes = ByteStringEncoding.decodePrimitiveBytes(any.getValue());
        var typeRef = objectMapper.getTypeFactory().constructCollectionType(collectionType, valueClass);
        return objectMapper.readValue(decodedBytes.toByteArray(), typeRef);
      } catch (JsonProcessingException e) {
        throw jsonProcessingException(valueClass, any, e);
      } catch (IOException e) {
        throw genericDecodeException(valueClass, any, e);
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

class DoneSerializer extends JsonSerializer<Done> {

  @Override
  public void serialize(Done value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    gen.writeEndObject();
  }
}

class DoneDeserializer extends JsonDeserializer<Done> {

  @Override
  public Done deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.currentToken() == JsonToken.START_OBJECT && p.nextToken() == JsonToken.END_OBJECT) {
      return Done.getInstance();
    } else {
      throw JsonMappingException.from(ctxt, "Cannot deserialize Done class, expecting empty object '{}'");
    }
  }
}