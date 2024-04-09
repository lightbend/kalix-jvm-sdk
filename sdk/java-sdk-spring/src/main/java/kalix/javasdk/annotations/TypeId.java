/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
