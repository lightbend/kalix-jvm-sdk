/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourced;

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
   * The name of the persistence id.
   *
   * <p>If not specified, defaults to the entities unqualified classname. It's strongly recommended
   * that you specify it explicitly.
   */
  String persistenceId() default "";

  /**
   * Specifies how snapshots of the entity state should be made: Zero means use default from
   * configuration file. (Default) Any negative value means never snapshot. Any positive value means
   * snapshot at-or-after that number of events.
   */
  int snapshotEvery() default 0;
}
