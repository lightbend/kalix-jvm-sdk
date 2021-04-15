/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

/**
 * Context for Replicated Entity creation.
 *
 * <p>This is available for injection into the constructor of a Replicated Entity.
 */
public interface ReplicatedEntityCreationContext
    extends ReplicatedEntityContext, ReplicatedDataFactory {}
