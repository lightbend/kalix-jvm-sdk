/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code @AkkaServerlessDescriptor} annotation is used in conjunction with the {@link
 * AkkaServerlessTest} annotation to mark the {@code AkkaServerless} that should be managed by the
 * Akka Serverless Testkit extension.
 *
 * @see AkkaServerlessTest
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AkkaServerlessDescriptor {}
