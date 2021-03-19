/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourcedentity;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An event sourced entity. */
@AkkaServerlessAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSourcedEntity {
  /**
   * The entity type name.
   *
   * <p>The entity's unqualified classname can be a good default. However, be aware that the chosen name must be stable through the entity lifecycle.  Never change it after deploying a
   * service that stored data of this type.
   */
  String entityType();

  /**
   * Specifies how snapshots of the entity state should be made: Zero means use default from
   * configuration file. (Default) Any negative value means never snapshot. Any positive value means
   * snapshot at-or-after that number of events.
   *
   * <p>It is strongly recommended to not disable snapshotting unless it is known that event sourced
   * entity will never have more than 100 events (in which case the default will anyway not trigger
   * any snapshots)
   */
  int snapshotEvery() default 0;
}
