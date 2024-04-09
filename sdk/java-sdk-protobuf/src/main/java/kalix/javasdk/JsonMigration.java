/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Allows to specify dedicated strategy for JSON schema evolution.
 * <p>
 * It is used when deserializing data of older version than the
 * {@link JsonMigration#currentVersion}. You implement the transformation of the
 * JSON structure in the {@link JsonMigration#transform} method. If you have changed the
 * class name you should add it to {@link JsonMigration#supportedClassNames}.
 */
public abstract class JsonMigration {

  /**
   * Define current version, that is, the value used when serializing new data. The first version, when no
   * migration was used, is always 0.
   */
  public abstract int currentVersion();

  /**
   * Define the supported forward version this migration can read (must be greater or equal than `currentVersion`).
   * If this value is different from {@link JsonMigration#currentVersion} a {@link JsonMigration#transform} will be used to downcast
   * the received payload to the current schema.
   */
  public int supportedForwardVersion() {
    return currentVersion();
  }

  /**
   * Implement the transformation of the incoming JSON structure to the current
   * JSON structure. The `JsonNode` is mutable so you can add and remove fields,
   * or change values. Note that you have to cast to specific sub-classes such
   * as `ObjectNode` and `ArrayNode` to get access to mutators.
   *
   * @param fromVersion the version of the old data
   * @param json        the incoming JSON data
   */
  public JsonNode transform(int fromVersion, JsonNode json) {
    return json;
  }

  /**
   * Override this method if you have changed the class name. Return
   * all old class names.
   */
  public List<String> supportedClassNames() {
    return List.of();
  }
}
