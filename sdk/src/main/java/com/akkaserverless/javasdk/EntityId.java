/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that the annotated parameter accepts an entity id.
 *
 * <p>This parameter may appear on handler methods and constructors for any class that provides
 * behavior for stateful service entity.
 *
 * <p>The type of the parameter must be {@link String}.
 */
@AkkaServerlessAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface EntityId {}
