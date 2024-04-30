/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import java.util.Collection;
import java.util.Set;
import kalix.replicatedentity.ReplicatedData;
/**
 * A Replicated Set that allows both the addition and removal of elements in a set.
 *
 * <p>A removal can only be done if all of the additions that added the key have been seen by this
 * node. This means that, for example if node 1 adds element A, and node 2 also adds element A, then
 * node 1's addition is replicated to node 3, and node 3 deletes it before node 2's addition is
 * replicated, then the element will still be in the map because node 2's addition had not yet been
 * observed by node 3, and will cause the element to be re-added when node 3 receives it. However,
 * if both * additions had been replicated to node 3, then the element will be removed.
 *
 * <p>Care needs to be taken to ensure that the serialized value of elements in the set is stable.
 * For example, if using protobufs, the serialized value of any maps contained in the protobuf is
 * not stable, and can yield a different set of bytes for the same logically equal element. Hence
 * maps should be avoided. Additionally, some changes in protobuf schemas which are backwards
 * compatible from a protobuf perspective, such as changing from sint32 to int32, do result in
 * different serialized bytes, and so must be avoided.
 *
 * @param <E> The type of elements.
 */
public interface ReplicatedSet<E> extends ReplicatedData, Iterable<E> {
  /**
   * Get the number of elements in this set (its cardinality).
   *
   * @return the number of elements in the set
   */
  int size();

  /**
   * Check whether this set is empty.
   *
   * @return {@code true} if this set contains no elements
   */
  boolean isEmpty();

  /**
   * Elements of this set as a regular {@link Set}.
   *
   * @return elements as {@link Set}
   */
  Set<E> elements();

  /**
   * Check whether this set contains the given element.
   *
   * @param element element whose presence in this set is to be tested
   * @return {@code true} if this set contains the specified element
   */
  boolean contains(E element);

  /**
   * Add an element to this set if it is not already present.
   *
   * @param element element to be added to this set
   * @return a new set with the additional element, or this unchanged set
   */
  ReplicatedSet<E> add(E element);

  /**
   * Remove an element from this set if it is present.
   *
   * @param element element to be removed from this set
   * @return a new set without the removed element, or this unchanged set
   */
  ReplicatedSet<E> remove(E element);

  /**
   * Check whether this set contains all the given elements.
   *
   * @param elements collection to be checked for containment in this set
   * @return {@code true} if this set contains all the given elements
   */
  boolean containsAll(Collection<E> elements);

  /**
   * Add elements to this set if they're not already present.
   *
   * <p>Effectively the <i>union</i> of two sets.
   *
   * @param elements collection containing elements to be added to this set
   * @return a new set with the additional elements, or this unchanged set
   */
  ReplicatedSet<E> addAll(Collection<E> elements);

  /**
   * Retain only the elements that are contained in the given collection.
   *
   * <p>Effectively the <i>intersection</i> of two sets.
   *
   * @param elements collection containing elements to be retained in this set
   * @return a new set with the retained elements, or this unchanged set
   */
  ReplicatedSet<E> retainAll(Collection<E> elements);

  /**
   * Remove elements from this set if they're present.
   *
   * <p>Effectively the <i>asymmetric set difference</i> of two sets.
   *
   * @param elements collection containing elements to be removed from this set
   * @return a new set without the removed elements, or this unchanged set
   */
  ReplicatedSet<E> removeAll(Collection<E> elements);

  /**
   * Remove all elements from this set.
   *
   * @return a new empty set
   */
  ReplicatedSet<E> clear();
}
