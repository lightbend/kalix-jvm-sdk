/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.valueentity;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A value based entity. */
@AkkaServerlessAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueEntity {
  /**
   * The entity type name
   *
   * <p>The entity's unqualified classname can be a good default. However, be aware that the chosen name must be stable through the entity lifecycle.  Never change it after deploying a
   * service that stored data of this type.
   */
  String entityType();
}
