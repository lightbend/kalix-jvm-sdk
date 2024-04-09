/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Indicates that Kalix should generate an entity key when this method is invoked.

 * The generated entity key is accessible via the entity context supplied in the SDK.
 * A method annotated with this annotation should not be annotated with {@link EntityKey},
 * if it does, an error will be raised.
 *
 * The generated key will be a Version 4 (random) UUID. The UUID will be generated using a cryptographically secure
 * random number generator.
 *
 * @deprecated Deprecated since v1.3.0. Use @GenerateId instead.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(since = "1.3.0")
public @interface GenerateEntityKey {

  Type value() default Type.VERSION_4_UUID;

  enum Type {
    /**
     * Generate a Version 4 (random) UUID. The UUID will be generated using a cryptographically secure random
     * number generator.
     */
    VERSION_4_UUID
  }
}
