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

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Assign a type to the entity. This name should be unique among the different existing entities
 * within a Kalix application.
 *
 * Additionally, the name should be stable as a different name means a different entity in storage. Changing this name
 * will create a new class of entity and all previous instances using the old name won't be accessible anymore.
 *
 * @deprecated Deprecated since v1.3.0. Use @TypeId instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(since = "1.3.0")
public @interface EntityType {
  String value();
}

