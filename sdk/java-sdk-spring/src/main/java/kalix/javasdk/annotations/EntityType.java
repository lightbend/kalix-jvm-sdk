/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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

