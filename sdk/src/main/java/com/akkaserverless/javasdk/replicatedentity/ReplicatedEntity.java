/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

import com.akkaserverless.javasdk.impl.AkkaServerlessAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A replicated entity.
 *
 * <p>Replicated entities store their state in a subclass of {@link ReplicatedData}. These may be
 * created using a {@link ReplicatedDataFactory}, which can be injected into the constructor or as a
 * parameter to any {@link CommandHandler} annotated method.
 *
 * <p>Only one Replicated Data object may be created for a Replicated Entity. It is important that
 * before creating the Replicated Data, the entity should check whether the Replicated Data has
 * already been created, for example, it may have been created on another node and replicated to
 * this node. To check, either use the {@link ReplicatedEntityContext#state(Class)} method, which
 * can be injected into the constructor or any {@link CommandHandler} method, or have an instance of
 * the Replicated Data wrapped in {@link java.util.Optional} injected into the constructor or
 * command handler methods.
 */
@AkkaServerlessAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReplicatedEntity {}
