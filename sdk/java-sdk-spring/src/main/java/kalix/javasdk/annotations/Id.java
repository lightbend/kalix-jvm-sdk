/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Assign an identifier to an entity or workflow instance.
 * This should be unique per entity/workflow instance and map to some field being received on the route path.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {

  String[] value();
}
