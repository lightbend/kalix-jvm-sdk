/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

import java.util.*;

/** Utility class for helping implement {@link ORMap} based replicated entities. */
abstract class AbstractORMapWrapper<K, V, D extends ReplicatedData> extends AbstractMap<K, V>
    implements Map<K, V> {

  final ORMap<K, D> ormap;

  AbstractORMapWrapper(ORMap<K, D> ormap) {
    this.ormap = ormap;
  }

  abstract V getValue(D data);

  abstract void setValue(D data, V value);

  abstract D getOrUpdateEntity(K key, V value);

  @Override
  public int size() {
    return ormap.size();
  }

  @Override
  public boolean containsKey(Object key) {
    return ormap.containsKey(key);
  }

  @Override
  public V get(Object key) {
    D data = ormap.get(key);
    if (data != null) {
      return getValue(data);
    } else {
      return null;
    }
  }

  @Override
  public V put(K key, V value) {
    D existing = ormap.get(key);
    if (existing != null) {
      V old = getValue(existing);
      setValue(existing, value);
      return old;
    } else {
      getOrUpdateEntity(key, value);
      return null;
    }
  }

  @Override
  public V remove(Object key) {
    D old = ormap.remove(key);
    if (old != null) {
      return getValue(old);
    } else {
      return null;
    }
  }

  @Override
  public void clear() {
    ormap.clear();
  }

  @Override
  public Set<K> keySet() {
    return ormap.keySet();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  private final class MapEntry implements Entry<K, V> {
    private final Entry<K, D> entry;

    MapEntry(Entry<K, D> entry) {
      this.entry = entry;
    }

    @Override
    public K getKey() {
      return entry.getKey();
    }

    @Override
    public V getValue() {
      return AbstractORMapWrapper.this.getValue(entry.getValue());
    }

    @Override
    public V setValue(V value) {
      V old = AbstractORMapWrapper.this.getValue(entry.getValue());
      AbstractORMapWrapper.this.setValue(entry.getValue(), value);
      return old;
    }
  }

  private final class EntrySet extends AbstractSet<Entry<K, V>> implements Set<Entry<K, V>> {
    @Override
    public int size() {
      return ormap.size();
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new Iterator<Entry<K, V>>() {
        private final Iterator<Entry<K, D>> iter = ormap.entrySet().iterator();

        @Override
        public boolean hasNext() {
          return iter.hasNext();
        }

        @Override
        public Entry<K, V> next() {
          return new MapEntry(iter.next());
        }

        @Override
        public void remove() {
          iter.remove();
        }
      };
    }

    @Override
    public boolean add(Entry<K, V> kvEntry) {
      return !kvEntry.getValue().equals(put(kvEntry.getKey(), kvEntry.getValue()));
    }

    @Override
    public void clear() {
      ormap.clear();
    }
  }
}
