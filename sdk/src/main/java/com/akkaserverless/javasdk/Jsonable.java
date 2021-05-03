/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that this class can be serialized to/from JSON. */
@AkkaServerlessAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Jsonable {}
