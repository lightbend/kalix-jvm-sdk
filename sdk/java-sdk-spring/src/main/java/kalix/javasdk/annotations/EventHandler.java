/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.annotations;

import java.lang.annotation.*;

/**
 * Annotation for marking a method under EventSourced entities as handlers for events of such
 * entity.
 *
 * <p>A method marked with this annotation needs to:
 *
 * <p>
 *
 * <ul>
 *   <li>be public
 *   <li>have exactly one parameter
 *   <li>have a return type that matches the state type of the enclosing entity
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventHandler {}
