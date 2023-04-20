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

package kalix.devtools.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import java.util.HashMap;
import java.util.Map;

public class DevModeSettings {

  private final static String portMappingsKeyPrefix = "kalix.dev-mode.service-port-mappings";

  public static String portMappingsKeyFor(String serviceName, String mapping) {
    return "-D" + portMappingsKeyPrefix + "." + serviceName + "=" + mapping;
  }

  public static boolean isPortMapping(String key) {
    return key.startsWith(portMappingsKeyPrefix);
  }

  public static String extractServiceName(String key) {
    return key.replace(portMappingsKeyPrefix + ".", "");
  }

  public static DevModeSettings fromConfig(Config config) {
    if (config.hasPath(portMappingsKeyPrefix)) {
      var configMappings = config.getConfig(portMappingsKeyPrefix);
      Map<String, String> portMappings = new HashMap<>();

      for (Map.Entry<String, ConfigValue> entry : configMappings.entrySet()) {
        if (entry.getValue().unwrapped() instanceof String) {
          var value = (String) entry.getValue().unwrapped();
          portMappings.put(entry.getKey(), value);
        } else {
          var fullKey = portMappingsKeyPrefix + "." + entry.getKey();
          throw new IllegalArgumentException(
            "Invalid config type. Settings '" + fullKey + "' should be of type String");
        }
      }
      return new DevModeSettings(portMappings);
    } else {
      return new DevModeSettings(new HashMap<>());
    }

  }

  public final Map<String, String> portMappings;

  public DevModeSettings(Map<String, String> portMappings) {
    this.portMappings = portMappings;
  }

}
