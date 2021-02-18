/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.entity;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A value based entity. */
@AkkaServerlessAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
  /**
   * The name of the persistence id.
   *
   * <p>If not specified, defaults to the entity's unqualified classname. It's strongly recommended
   * that you specify it explicitly.
   */
  String persistenceId() default "";
}
