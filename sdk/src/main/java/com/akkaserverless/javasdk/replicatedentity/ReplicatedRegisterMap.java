/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.replicatedentity;

import java.util.Optional;
import java.util.Set;

/**
 * A Map of registers. Uses {@link ReplicatedRegister}'s as values.
 *
 * @param <K> The type for keys.
 * @param <V> The type for values.
 */
public final class ReplicatedRegisterMap<K, V> implements ReplicatedData {

  private final ORMap<K, ReplicatedRegister<V>> ormap;

  public ReplicatedRegisterMap(ORMap<K, ReplicatedRegister<V>> ormap) {
    this.ormap = ormap;
  }

  Optional<V> getValue(K key) {
    ReplicatedRegister<V> register = ormap.get(key);
    if (register == null) return Optional.empty();
    else return Optional.ofNullable(register.get());
  }

  void setValue(K key, V value) {
    ReplicatedRegister<V> register = ormap.get(key);
    if (register == null) ormap.getOrCreate(key, f -> f.newRegister(value));
    else register.set(value);
  }

  public Set<K> keySet() {
    return ormap.keySet();
  }

  public int size() {
    return ormap.size();
  }

  public boolean isEmpty() {
    return ormap.isEmpty();
  }

  public boolean containsKey(K key) {
    return ormap.containsKey(key);
  }

  public void remove(K key) {
    ormap.remove(key);
  }
}
