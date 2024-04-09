/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;

/**
 * Factory for creating Replicated Data objects.
 *
 * <p>This is used both by Replicated Entity contexts that allow creating Replicated Data objects,
 * and by Replicated Data objects that allow nesting other Replicated Data.
 *
 * <p>Replicated Data objects may only be created by a supplied Replicated Data factory. Replicated
 * Data objects created any other way will not be known by the library and so won't have their
 * deltas synced to and from the proxy.
 */
public interface ReplicatedDataFactory {

  /** Create a new counter. */
  ReplicatedCounter newCounter();

  /** Create a new map of counters. */
  <K> ReplicatedCounterMap<K> newReplicatedCounterMap();

  /** Create a new ReplicatedSet. */
  <T> ReplicatedSet<T> newReplicatedSet();

  /** Create a new multimap (map of sets). */
  <K, V> ReplicatedMultiMap<K, V> newReplicatedMultiMap();

  /** Create a new ReplicatedRegister. */
  <T> ReplicatedRegister<T> newRegister(T value);

  /** Create a new map of registers. */
  <K, V> ReplicatedRegisterMap<K, V> newReplicatedRegisterMap();

  /** Create a new ReplicatedMap. */
  <K, V extends ReplicatedData> ReplicatedMap<K, V> newReplicatedMap();

  /** Create a new Vote. */
  ReplicatedVote newVote();
}
