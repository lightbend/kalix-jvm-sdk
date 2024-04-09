/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Assign a key to the entity.
 * This should be unique per entity and map to some field being received on the route path.
 *
 * @deprecated Deprecated since v1.3.0. Use @Id instead.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(since = "1.3.0")
public @interface EntityKey {

  /**
   */
  String[] value();
}
