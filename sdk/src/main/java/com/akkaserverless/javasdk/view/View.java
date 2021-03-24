/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Transformation of incoming messages and previous state to updated state can be implemented in a
 * class annotated with <code>@View</code>. It must be registered with {@link
 * com.akkaserverless.javasdk.AkkaServerless#registerView AkkaServerless.registerView}.
 *
 * <p>The view class should define methods corresponding to the service calls (rpc) for view
 * updates. Those methods are annoted with {@link Handler}.
 */
@AkkaServerlessAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface View {}
