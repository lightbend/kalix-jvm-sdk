/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Mark annotation for all AkkaServerless annotations */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AkkaServerlessAnnotation {}
