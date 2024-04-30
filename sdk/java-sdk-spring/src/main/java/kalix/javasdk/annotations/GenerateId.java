/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Indicates that Kalix should generate an id when this method is invoked.

 * The generated id is accessible via the entity/workflow context supplied in the SDK.
 * A method annotated with this annotation should not be annotated with {@link Id},
 * if it does, an error will be raised.
 *
 * The generated key will be a Version 4 (random) UUID. The UUID will be generated using a cryptographically secure
 * random number generator.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenerateId {

  Type value() default Type.VERSION_4_UUID;

  enum Type {
    /**
     * Generate a Version 4 (random) UUID. The UUID will be generated using a cryptographically secure random
     * number generator.
     */
    VERSION_4_UUID
  }
}
