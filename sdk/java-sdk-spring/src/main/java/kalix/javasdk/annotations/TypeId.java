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
 * Assign a type identifier to an entity or workflow.
 * The type identifier should be unique among the different workflows and entities within a Kalix application.
 *
 * Additionally, the TypeId should be stable as a different identifier means a different workflow/entity in storage.
 * Changing this identifier will create a new class of component and all previous instances using
 * the old identifier won't be accessible anymore.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeId {
  String value();
}
