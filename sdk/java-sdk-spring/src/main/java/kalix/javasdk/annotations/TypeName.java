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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to assign a logical type name to events.
 *
 * Kalix needs to identify each event in order to deliver them to the right event handlers.
 * If a logical type name isn't specified, Kalix will use the fully qualified class name.
 *
 * Once an event is persisted in Kalix, you won't be able to rename your class if no logical type name 
 * has been specified, as Kalix won't be able to recognize previously persisted events. 
 * 
 * Therefore, we recommend all event classes use a logical type name.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeName {

  /** Logical type name for the annotated type.
   * If missing (or defined as Empty String), the fully qualified class name will be used.
   */
  String value() default "";
}
