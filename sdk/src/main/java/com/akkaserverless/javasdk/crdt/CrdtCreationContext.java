/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

/**
 * Context for CRDT creation.
 *
 * <p>This is available for injection into the constructor of a CRDT.
 */
public interface CrdtCreationContext extends CrdtContext, CrdtFactory {}
