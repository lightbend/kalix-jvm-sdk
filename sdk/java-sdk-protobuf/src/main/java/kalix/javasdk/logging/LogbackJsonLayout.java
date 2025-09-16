/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * This Logback JSON layout uses the name `severity` (instead of `level`).
 *
 * <p>Eg. Stackdriver expects the log severity to be in a field called `severity`.
 *
 * <p>IMPORTANT: This class depends on the "logback-json-classic" library (organization
 * "ch.qos.logback.contrib") and the Jackson layout support requires "logback-jackson" (organization
 * "ch.qos.logback.contrib") which need to be added as dependencies.
 */
public class LogbackJsonLayout extends ch.qos.logback.contrib.json.classic.JsonLayout {

  private final static String KVP_ATTR_NAME = "kvpList";

  public LogbackJsonLayout() {
    setIncludeLevel(false);
  }

  @Override
  public void addCustomDataToJsonMap(
      java.util.Map<String, Object> map, ch.qos.logback.classic.spi.ILoggingEvent event) {
    add("severity", true, String.valueOf(event.getLevel()), map);

    if (event.getKeyValuePairs() != null && !event.getKeyValuePairs().isEmpty()) {
      Map<String, Object> kvp = new HashMap<>();
      event.getKeyValuePairs().forEach(keyValuePair ->
          kvp.put(keyValuePair.key, keyValuePair.value)
      );
      addMap(KVP_ATTR_NAME, true, kvp, map);
    }
  }
}
