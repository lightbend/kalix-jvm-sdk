/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

/**
 * Factory for creating Replicated Data objects.
 *
 * <p>This is used both by Replicated Entity contexts that allow creating Replicated Data objects,
 * as well as by Replicated Data objects that allow nesting other Replicated Data.
 *
 * <p>Replicated Data objects may only be created by a supplied Replicated Data factory. Replicated
 * Data objects created any other way will not be known by the library and so won't have their
 * deltas synced to and from the proxy.
 */
public interface ReplicatedDataFactory {
  /**
   * Create a new GCounter.
   *
   * @return The new GCounter.
   */
  GCounter newGCounter();

  /**
   * Create a new PNCounter.
   *
   * @return The new PNCounter.
   */
  PNCounter newPNCounter();

  /**
   * Create a new GSet.
   *
   * @return The new GSet.
   */
  <T> GSet<T> newGSet();

  /**
   * Create a new ORSet.
   *
   * @return The new ORSet.
   */
  <T> ORSet<T> newORSet();

  /**
   * Create a new Flag.
   *
   * @return The new Flag.
   */
  Flag newFlag();

  /**
   * Create a new LWWRegister.
   *
   * @return The new LWWRegister.
   */
  <T> LWWRegister<T> newLWWRegister(T value);

  /**
   * Create a new ORMap.
   *
   * @return The new ORMap.
   */
  <K, V extends ReplicatedData> ORMap<K, V> newORMap();

  /**
   * Create a new Vote.
   *
   * @return The new Vote.
   */
  Vote newVote();
}
