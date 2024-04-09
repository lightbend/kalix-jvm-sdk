/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
